package com.example.workops.admin.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.admin.user.form.UserEditForm;
import com.example.workops.admin.user.form.UserForm;
import com.example.workops.admin.user.mapper.UserAdminMapper;
import com.example.workops.admin.user.model.UserEditTarget;
import com.example.workops.admin.user.model.UserListItem;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(UserAdminServiceTests.UserAdminServiceTestConfig.class)
class UserAdminServiceTests {

    private static final Long COMPANY_ID = 1L;
    private static final Long PLATFORM_USER_ID = 7L;
    private static final Long MANAGER_USER_ID = 3L;
    private static final Long USER_ID = 20L;
    private static final Long DEPARTMENT_ID = 10L;
    private static final String COGNITO_SUB = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private UserAdminMapper userAdminMapper;

    @Autowired
    private CognitoUserProvisioner cognitoUserProvisioner;

    @BeforeEach
    void setUp() {
        reset(userAdminMapper, cognitoUserProvisioner);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformAdminCanFindAllUsers() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        UserListItem platformUser = userListItem(PLATFORM_USER_ID, "platform-admin", "PLATFORM", "WorkOps");
        UserListItem tenantUser = userListItem(MANAGER_USER_ID, "kthm-manager", "TENANT", "北浜精密機器株式会社");
        when(userAdminMapper.findPlatformUsers()).thenReturn(List.of(platformUser, tenantUser));

        List<UserListItem> result = userAdminService.findPlatformUsers();

        assertThat(result).containsExactly(platformUser, tenantUser);
        verify(userAdminMapper).findPlatformUsers();
    }

    @Test
    void tenantManagerCanFindOwnCompanyTenantUsers() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        UserListItem manager = userListItem(MANAGER_USER_ID, "kthm-manager", "TENANT", "北浜精密機器株式会社");
        when(userAdminMapper.findTenantUsersByCompanyId(COMPANY_ID)).thenReturn(List.of(manager));

        List<UserListItem> result = userAdminService.findTenantUsers();

        assertThat(result).containsExactly(manager);
        verify(userAdminMapper).findTenantUsersByCompanyId(COMPANY_ID);
    }

    @Test
    void tenantManagerCannotFindOtherCompanyOrPlatformUserDetail() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.empty());
        when(userAdminMapper.findTenantUserByIdAndCompanyId(PLATFORM_USER_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.findTenantUserDetail(USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> userAdminService.findTenantUserDetail(PLATFORM_USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void platformAdminCanCreatePlatformUser() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(userAdminMapper.existsPlatformUsername("new-platform")).thenReturn(false);
        when(userAdminMapper.existsPlatformEmail("new-platform@example.local")).thenReturn(false);
        when(cognitoUserProvisioner.provision(new CognitoUserProvisionRequest("new-platform", "new-platform@example.local")))
                .thenReturn(new ProvisionedCognitoUser(COGNITO_SUB));
        when(userAdminMapper.findLastInsertId()).thenReturn(USER_ID);

        Long result = userAdminService.createPlatformUser(platformUserForm());

        assertThat(result).isEqualTo(USER_ID);
        verify(cognitoUserProvisioner).provision(new CognitoUserProvisionRequest("new-platform", "new-platform@example.local"));
        verify(userAdminMapper).insertUser(
                null,
                null,
                COGNITO_SUB,
                "new-platform",
                "新PLATFORM",
                "new-platform@example.local",
                "PLATFORM",
                PLATFORM_USER_ID,
                PLATFORM_USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "PLATFORM_ADMIN");
    }

    @Test
    void platformAdminCanCreateTenantUserForAnyCompany() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        givenActiveCompanyAndDepartment();
        when(userAdminMapper.existsTenantUsername(COMPANY_ID, "new-tenant")).thenReturn(false);
        when(userAdminMapper.existsTenantEmail(COMPANY_ID, "new-tenant@example.local")).thenReturn(false);
        when(cognitoUserProvisioner.provision(new CognitoUserProvisionRequest("new-tenant", "new-tenant@example.local")))
                .thenReturn(new ProvisionedCognitoUser(COGNITO_SUB));
        when(userAdminMapper.findLastInsertId()).thenReturn(USER_ID);

        Long result = userAdminService.createPlatformUser(tenantUserForm(List.of("TENANT_MANAGER"), DEPARTMENT_ID));

        assertThat(result).isEqualTo(USER_ID);
        verify(userAdminMapper).insertUser(
                COMPANY_ID,
                DEPARTMENT_ID,
                COGNITO_SUB,
                "new-tenant",
                "新TENANT",
                "new-tenant@example.local",
                "TENANT",
                PLATFORM_USER_ID,
                PLATFORM_USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "TENANT_MANAGER");
    }

    @Test
    void initialTenantManagerIsCreatedWithTenantManagerPermission() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));
        when(userAdminMapper.existsTenantUsername(COMPANY_ID, "initial-manager")).thenReturn(false);
        when(userAdminMapper.existsTenantEmail(COMPANY_ID, "initial-manager@example.local")).thenReturn(false);
        when(cognitoUserProvisioner.provision(new CognitoUserProvisionRequest("initial-manager", "initial-manager@example.local")))
                .thenReturn(new ProvisionedCognitoUser(COGNITO_SUB));
        when(userAdminMapper.findLastInsertId()).thenReturn(USER_ID);

        Long result = userAdminService.createInitialTenantManager(
                COMPANY_ID,
                "initial-manager",
                "初期 管理者",
                "initial-manager@example.local");

        assertThat(result).isEqualTo(USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "TENANT_MANAGER");
        verify(userAdminMapper).insertUser(
                COMPANY_ID,
                null,
                COGNITO_SUB,
                "initial-manager",
                "初期 管理者",
                "initial-manager@example.local",
                "TENANT",
                PLATFORM_USER_ID,
                PLATFORM_USER_ID);
    }

    @Test
    void tenantManagerCanCreateOwnCompanyTenantUser() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(DEPARTMENT_ID));
        when(userAdminMapper.existsTenantUsername(COMPANY_ID, "new-tenant")).thenReturn(false);
        when(userAdminMapper.existsTenantEmail(COMPANY_ID, "new-tenant@example.local")).thenReturn(false);
        when(cognitoUserProvisioner.provision(new CognitoUserProvisionRequest("new-tenant", "new-tenant@example.local")))
                .thenReturn(new ProvisionedCognitoUser(COGNITO_SUB));
        when(userAdminMapper.findLastInsertId()).thenReturn(USER_ID);

        Long result = userAdminService.createTenantUser(
                new UserForm("PLATFORM", 999L, DEPARTMENT_ID, "new-tenant", "新TENANT",
                        "new-tenant@example.local", List.of("TENANT_EDITOR")));

        assertThat(result).isEqualTo(USER_ID);
        verify(userAdminMapper).insertUser(
                COMPANY_ID,
                DEPARTMENT_ID,
                COGNITO_SUB,
                "new-tenant",
                "新TENANT",
                "new-tenant@example.local",
                "TENANT",
                MANAGER_USER_ID,
                MANAGER_USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "TENANT_EDITOR");
    }

    @Test
    void tenantManagerCannotAssignPlatformAdminBeforeCognitoProvision() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));

        assertThatThrownBy(() -> userAdminService.createTenantUser(
                new UserForm("TENANT", COMPANY_ID, null, "new-tenant", "新TENANT",
                        "new-tenant@example.local", List.of("PLATFORM_ADMIN"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cognitoUserProvisioner);
    }

    @Test
    void tenantManagerCannotUseOtherCompanyOrDeletedDepartmentBeforeCognitoProvision() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.createTenantUser(
                new UserForm("TENANT", COMPANY_ID, DEPARTMENT_ID, "new-tenant", "新TENANT",
                        "new-tenant@example.local", List.of("TENANT_MANAGER"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cognitoUserProvisioner);
    }

    @Test
    void tenantManagerDuplicateUsernameAndEmailAreRejectedBeforeCognitoProvision() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));
        when(userAdminMapper.existsTenantUsername(COMPANY_ID, "new-tenant")).thenReturn(true);

        assertThatThrownBy(() -> userAdminService.createTenantUser(tenantUserForm(List.of("TENANT_MANAGER"), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        reset(userAdminMapper);
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));
        when(userAdminMapper.existsTenantUsername(COMPANY_ID, "new-tenant")).thenReturn(false);
        when(userAdminMapper.existsTenantEmail(COMPANY_ID, "new-tenant@example.local")).thenReturn(true);

        assertThatThrownBy(() -> userAdminService.createTenantUser(tenantUserForm(List.of("TENANT_MANAGER"), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cognitoUserProvisioner);
    }

    @Test
    void invalidPermissionSetCombinationIsRejectedBeforeCognitoProvision() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));

        assertThatThrownBy(() -> userAdminService.createPlatformUser(
                new UserForm("PLATFORM", null, null, "new-platform", "新PLATFORM",
                        "new-platform@example.local", List.of("TENANT_MANAGER"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cognitoUserProvisioner);
    }

    @Test
    void tenantUserWithOtherCompanyDepartmentIsRejectedBeforeCognitoProvision() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.createPlatformUser(tenantUserForm(List.of("TENANT_MANAGER"), DEPARTMENT_ID)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cognitoUserProvisioner);
    }

    @Test
    void duplicateUsernameAndEmailAreRejectedBeforeCognitoProvision() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(userAdminMapper.existsPlatformUsername("new-platform")).thenReturn(true);

        assertThatThrownBy(() -> userAdminService.createPlatformUser(platformUserForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        reset(userAdminMapper);
        when(userAdminMapper.existsPlatformUsername("new-platform")).thenReturn(false);
        when(userAdminMapper.existsPlatformEmail("new-platform@example.local")).thenReturn(true);
        assertThatThrownBy(() -> userAdminService.createPlatformUser(platformUserForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cognitoUserProvisioner);
    }

    @Test
    void cognitoProvisionFailureDoesNotInsertUser() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(userAdminMapper.existsPlatformUsername("new-platform")).thenReturn(false);
        when(userAdminMapper.existsPlatformEmail("new-platform@example.local")).thenReturn(false);
        when(cognitoUserProvisioner.provision(new CognitoUserProvisionRequest("new-platform", "new-platform@example.local")))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cognitoユーザー作成に失敗しました。"));

        assertThatThrownBy(() -> userAdminService.createPlatformUser(platformUserForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
        verify(userAdminMapper, never()).insertUser(
                null,
                null,
                COGNITO_SUB,
                "new-platform",
                "新PLATFORM",
                "new-platform@example.local",
                "PLATFORM",
                PLATFORM_USER_ID,
                PLATFORM_USER_ID);
    }

    @Test
    void platformAdminCanUpdatePlatformUser() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(userAdminMapper.findPlatformUserEditTarget(USER_ID)).thenReturn(Optional.of(platformEditTarget()));
        when(userAdminMapper.existsPlatformEmailExcludingUser(USER_ID, "updated-platform@example.local")).thenReturn(false);

        userAdminService.updatePlatformUser(
                USER_ID,
                new UserEditForm("更新PLATFORM", "updated-platform@example.local", null, List.of("PLATFORM_ADMIN")));

        verify(userAdminMapper).updatePlatformUserEditableFields(
                USER_ID,
                "更新PLATFORM",
                "updated-platform@example.local",
                null,
                PLATFORM_USER_ID);
        verify(userAdminMapper).deleteUserPermissionSets(USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "PLATFORM_ADMIN");
        verifyNoInteractions(cognitoUserProvisioner);
    }

    @Test
    void platformAdminCanUpdateTenantUser() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(userAdminMapper.findPlatformUserEditTarget(USER_ID)).thenReturn(Optional.of(tenantEditTarget()));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(Optional.of(DEPARTMENT_ID));
        when(userAdminMapper.existsTenantEmailExcludingUser(COMPANY_ID, USER_ID, "updated-tenant@example.local")).thenReturn(false);
        when(userAdminMapper.countActiveTenantManagersByCompanyId(COMPANY_ID)).thenReturn(1);

        userAdminService.updatePlatformUser(
                USER_ID,
                new UserEditForm("更新TENANT", "updated-tenant@example.local", DEPARTMENT_ID, List.of("TENANT_EDITOR")));

        verify(userAdminMapper).updatePlatformUserEditableFields(
                USER_ID,
                "更新TENANT",
                "updated-tenant@example.local",
                DEPARTMENT_ID,
                PLATFORM_USER_ID);
        verify(userAdminMapper).deleteUserPermissionSets(USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "TENANT_EDITOR");
    }

    @Test
    void tenantManagerCanUpdateOwnCompanyTenantUser() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.of(tenantEditTarget()));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(Optional.of(DEPARTMENT_ID));
        when(userAdminMapper.existsTenantEmailExcludingUser(COMPANY_ID, USER_ID, "updated-tenant@example.local")).thenReturn(false);
        when(userAdminMapper.countActiveTenantManagersByCompanyId(COMPANY_ID)).thenReturn(1);

        userAdminService.updateTenantUser(
                USER_ID,
                new UserEditForm("更新TENANT", "updated-tenant@example.local", DEPARTMENT_ID, List.of("TENANT_MANAGER")));

        verify(userAdminMapper).updateTenantUserEditableFields(
                USER_ID,
                COMPANY_ID,
                "更新TENANT",
                "updated-tenant@example.local",
                DEPARTMENT_ID,
                MANAGER_USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "TENANT_MANAGER");
    }

    @Test
    void tenantManagerCannotUpdateOtherCompanyOrPlatformUser() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.empty());
        when(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(PLATFORM_USER_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.updateTenantUser(USER_ID, editForm(List.of("TENANT_MANAGER"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> userAdminService.updateTenantUser(PLATFORM_USER_ID, editForm(List.of("TENANT_MANAGER"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void invalidUpdatePermissionIsRejected() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.of(tenantEditTarget()));

        assertThatThrownBy(() -> userAdminService.updateTenantUser(USER_ID, editForm(List.of("PLATFORM_ADMIN"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(userAdminMapper, never()).updateTenantUserEditableFields(
                USER_ID,
                COMPANY_ID,
                "更新ユーザー",
                "updated@example.local",
                DEPARTMENT_ID,
                MANAGER_USER_ID);
    }

    @Test
    void invalidDepartmentAndDuplicateEmailAreRejectedOnUpdate() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.of(tenantEditTarget()));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.updateTenantUser(USER_ID, editForm(List.of("TENANT_MANAGER"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        reset(userAdminMapper);
        when(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.of(tenantEditTarget()));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(Optional.of(DEPARTMENT_ID));
        when(userAdminMapper.existsTenantEmailExcludingUser(COMPANY_ID, USER_ID, "updated@example.local")).thenReturn(true);

        assertThatThrownBy(() -> userAdminService.updateTenantUser(USER_ID, editForm(List.of("TENANT_MANAGER"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void tenantManagerCountZeroIsRejectedAsBusinessValidation() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.of(tenantEditTarget()));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(Optional.of(DEPARTMENT_ID));
        when(userAdminMapper.existsTenantEmailExcludingUser(COMPANY_ID, USER_ID, "updated@example.local")).thenReturn(false);
        when(userAdminMapper.countActiveTenantManagersByCompanyId(COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> userAdminService.updateTenantUser(USER_ID, editForm(List.of("TENANT_EDITOR"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(userAdminMapper).deleteUserPermissionSets(USER_ID);
        verify(userAdminMapper).insertUserPermissionSetByCode(USER_ID, "TENANT_EDITOR");
    }

    @Test
    void tenantViewerAndEditorCannotInvokeUserAdminService() {
        signIn(1L, COMPANY_ID, "TENANT", permission("TENANT_VIEWER", "閲覧者"));
        assertThatThrownBy(() -> userAdminService.findTenantUsers()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.createPlatformUser(platformUserForm())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.createTenantUser(tenantUserForm(List.of("TENANT_MANAGER"), null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.findTenantUserEditForm(USER_ID)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.updateTenantUser(USER_ID, editForm(List.of("TENANT_MANAGER"))))
                .isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.clearContext();
        signIn(2L, COMPANY_ID, "TENANT", permission("TENANT_EDITOR", "編集者"));
        assertThatThrownBy(() -> userAdminService.findTenantUsers()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.createPlatformUser(platformUserForm())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.createTenantUser(tenantUserForm(List.of("TENANT_MANAGER"), null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.findTenantUserEditForm(USER_ID)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.updateTenantUser(USER_ID, editForm(List.of("TENANT_MANAGER"))))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userAdminMapper);
        verifyNoInteractions(cognitoUserProvisioner);
    }

    private void givenActiveCompanyAndDepartment() {
        when(userAdminMapper.findActiveCompanyId(COMPANY_ID)).thenReturn(Optional.of(COMPANY_ID));
        when(userAdminMapper.findActiveDepartmentIdByCompanyId(DEPARTMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(DEPARTMENT_ID));
    }

    private void signIn(Long userId, Long companyId, String actorType, PermissionSetContext permissionSet) {
        LoginUserContext loginUserContext = new LoginUserContext(
                userId,
                "test-user",
                "test-user@example.local",
                actorType,
                companyId,
                List.of(permissionSet));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loginUserContext,
                null,
                List.of(new SimpleGrantedAuthority(permissionSet.code()))));
    }

    private PermissionSetContext permission(String code, String name) {
        return new PermissionSetContext(code, name);
    }

    private UserForm platformUserForm() {
        return new UserForm(
                "PLATFORM",
                null,
                null,
                "new-platform",
                "新PLATFORM",
                "new-platform@example.local",
                List.of("PLATFORM_ADMIN"));
    }

    private UserForm tenantUserForm(List<String> permissionSetCodes, Long departmentId) {
        return new UserForm(
                "TENANT",
                COMPANY_ID,
                departmentId,
                "new-tenant",
                "新TENANT",
                "new-tenant@example.local",
                permissionSetCodes);
    }

    private UserEditForm editForm(List<String> permissionSetCodes) {
        return new UserEditForm(
                "更新ユーザー",
                "updated@example.local",
                DEPARTMENT_ID,
                permissionSetCodes);
    }

    private UserEditTarget platformEditTarget() {
        return new UserEditTarget(
                USER_ID,
                null,
                null,
                "platform-user",
                "PLATFORMユーザー",
                "platform-user@example.local",
                "PLATFORM",
                "WorkOps",
                COGNITO_SUB);
    }

    private UserEditTarget tenantEditTarget() {
        return new UserEditTarget(
                USER_ID,
                COMPANY_ID,
                DEPARTMENT_ID,
                "tenant-user",
                "TENANTユーザー",
                "tenant-user@example.local",
                "TENANT",
                "北浜精密機器株式会社",
                COGNITO_SUB);
    }

    private UserListItem userListItem(Long userId, String username, String actorType, String companyName) {
        return new UserListItem(
                userId,
                username,
                "表示名",
                username + "@example.local",
                actorType,
                companyName,
                "総務部（削除済み）",
                "TENANT_MANAGER / 管理者",
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    @Configuration
    @EnableMethodSecurity
    static class UserAdminServiceTestConfig {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new CurrentUserProvider();
        }

        @Bean
        UserAdminMapper userAdminMapper() {
            return mock(UserAdminMapper.class);
        }

        @Bean
        CognitoUserProvisioner cognitoUserProvisioner() {
            return mock(CognitoUserProvisioner.class);
        }

        @Bean
        UserAdminService userAdminService(
                CurrentUserProvider currentUserProvider,
                UserAdminMapper userAdminMapper,
                CognitoUserProvisioner cognitoUserProvisioner) {
            return new UserAdminService(
                    currentUserProvider,
                    userAdminMapper,
                    cognitoUserProvisioner);
        }
    }
}
