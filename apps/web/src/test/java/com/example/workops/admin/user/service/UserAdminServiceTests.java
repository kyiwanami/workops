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

import com.example.workops.admin.user.mapper.UserAdminMapper;
import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserListItem;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private UserAdminMapper userAdminMapper;

    @BeforeEach
    void setUp() {
        reset(userAdminMapper);
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
    void platformAdminCanFindPlatformUserDetail() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        UserDetail detail = platformUserDetail();
        when(userAdminMapper.findPlatformUserById(PLATFORM_USER_ID)).thenReturn(Optional.of(detail));

        UserDetail result = userAdminService.findPlatformUserDetail(PLATFORM_USER_ID);

        assertThat(result).isEqualTo(detail);
        verify(userAdminMapper).findPlatformUserById(PLATFORM_USER_ID);
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
    void tenantManagerCanFindOwnCompanyTenantUserDetail() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        UserDetail detail = tenantUserDetail(MANAGER_USER_ID, COMPANY_ID);
        when(userAdminMapper.findTenantUserByIdAndCompanyId(MANAGER_USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(detail));

        UserDetail result = userAdminService.findTenantUserDetail(MANAGER_USER_ID);

        assertThat(result).isEqualTo(detail);
        verify(userAdminMapper).findTenantUserByIdAndCompanyId(MANAGER_USER_ID, COMPANY_ID);
    }

    @Test
    void tenantManagerCannotFindOtherCompanyUserDetail() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserByIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.findTenantUserDetail(USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(userAdminMapper).findTenantUserByIdAndCompanyId(USER_ID, COMPANY_ID);
    }

    @Test
    void tenantManagerCannotFindPlatformUserDetail() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
        when(userAdminMapper.findTenantUserByIdAndCompanyId(PLATFORM_USER_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.findTenantUserDetail(PLATFORM_USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(userAdminMapper).findTenantUserByIdAndCompanyId(PLATFORM_USER_ID, COMPANY_ID);
    }

    @Test
    void permissionSetDisplayIsReturnedInListAndDetail() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        UserListItem listItem = userListItem(MANAGER_USER_ID, "kthm-manager", "TENANT", "北浜精密機器株式会社");
        UserDetail detail = tenantUserDetail(MANAGER_USER_ID, COMPANY_ID);
        when(userAdminMapper.findPlatformUsers()).thenReturn(List.of(listItem));
        when(userAdminMapper.findPlatformUserById(MANAGER_USER_ID)).thenReturn(Optional.of(detail));

        assertThat(userAdminService.findPlatformUsers())
                .extracting(UserListItem::permissionSetDisplay)
                .containsExactly("TENANT_MANAGER / 管理者");
        assertThat(userAdminService.findPlatformUserDetail(MANAGER_USER_ID).permissionSetDisplay())
                .isEqualTo("TENANT_MANAGER / 管理者");
    }

    @Test
    void departmentDisplayIsReturnedInListAndDetail() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        UserListItem deletedDepartmentUser = userListItem(USER_ID, "deleted-dept-user", "TENANT", "北浜精密機器株式会社");
        UserDetail noDepartmentUser = tenantUserDetail(USER_ID, COMPANY_ID);
        when(userAdminMapper.findPlatformUsers()).thenReturn(List.of(deletedDepartmentUser));
        when(userAdminMapper.findPlatformUserById(USER_ID)).thenReturn(Optional.of(noDepartmentUser));

        assertThat(userAdminService.findPlatformUsers())
                .extracting(UserListItem::departmentName)
                .containsExactly("総務部（削除済み）");
        assertThat(userAdminService.findPlatformUserDetail(USER_ID).departmentName()).isEqualTo("未設定");
    }

    @Test
    void tenantViewerAndEditorCannotInvokeUserAdminService() {
        signIn(1L, COMPANY_ID, "TENANT", permission("TENANT_VIEWER", "閲覧者"));
        assertThatThrownBy(() -> userAdminService.findTenantUsers()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.findTenantUserDetail(MANAGER_USER_ID)).isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.clearContext();
        signIn(2L, COMPANY_ID, "TENANT", permission("TENANT_EDITOR", "編集者"));
        assertThatThrownBy(() -> userAdminService.findTenantUsers()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.findTenantUserDetail(MANAGER_USER_ID)).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userAdminMapper);
    }

    @Test
    void tenantManagerCannotInvokePlatformMethods() {
        signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));

        assertThatThrownBy(() -> userAdminService.findPlatformUsers()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userAdminService.findPlatformUserDetail(USER_ID)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(userAdminMapper);
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

    private UserDetail platformUserDetail() {
        return new UserDetail(
                PLATFORM_USER_ID,
                null,
                "platform-admin",
                "WorkOps 管理者",
                "platform-admin@example.local",
                "PLATFORM",
                "WorkOps",
                "未設定",
                "PLATFORM_ADMIN / WorkOps管理者",
                "00000000-0000-0000-0000-000000000000",
                false,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    private UserDetail tenantUserDetail(Long userId, Long companyId) {
        return new UserDetail(
                userId,
                companyId,
                "kthm-manager",
                "北浜 管理者",
                "kthm-manager@example.local",
                "TENANT",
                "北浜精密機器株式会社",
                "未設定",
                "TENANT_MANAGER / 管理者",
                "00000000-0000-0000-0000-000000000003",
                false,
                LocalDateTime.of(2026, 5, 1, 9, 0),
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
        UserAdminService userAdminService(
                CurrentUserProvider currentUserProvider,
                UserAdminMapper userAdminMapper) {
            return new UserAdminService(currentUserProvider, userAdminMapper);
        }
    }
}
