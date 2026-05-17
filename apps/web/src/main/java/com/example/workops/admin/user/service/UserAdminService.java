package com.example.workops.admin.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.admin.user.form.UserForm;
import com.example.workops.admin.user.mapper.UserAdminMapper;
import com.example.workops.admin.user.model.CompanySelectOption;
import com.example.workops.admin.user.model.DepartmentSelectOption;
import com.example.workops.admin.user.model.PermissionSetOption;
import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserListItem;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;

/**
 * PLATFORM_ADMINとTENANT_MANAGER向けユーザー参照ユースケースを扱うService。
 */
@Service
public class UserAdminService {

    private static final String ACTOR_TYPE_PLATFORM = "PLATFORM";
    private static final String ACTOR_TYPE_TENANT = "TENANT";
    private static final String PERMISSION_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String PERMISSION_TENANT_VIEWER = "TENANT_VIEWER";
    private static final String PERMISSION_TENANT_EDITOR = "TENANT_EDITOR";
    private static final String PERMISSION_TENANT_MANAGER = "TENANT_MANAGER";
    private static final Set<String> PLATFORM_PERMISSION_CODES = Set.of(PERMISSION_PLATFORM_ADMIN);
    private static final Set<String> TENANT_PERMISSION_CODES = Set.of(
            PERMISSION_TENANT_VIEWER,
            PERMISSION_TENANT_EDITOR,
            PERMISSION_TENANT_MANAGER);

    private final CurrentUserProvider currentUserProvider;
    private final UserAdminMapper userAdminMapper;
    private final CognitoUserProvisioner cognitoUserProvisioner;

    public UserAdminService(
            CurrentUserProvider currentUserProvider,
            UserAdminMapper userAdminMapper,
            CognitoUserProvisioner cognitoUserProvisioner) {
        this.currentUserProvider = currentUserProvider;
        this.userAdminMapper = userAdminMapper;
        this.cognitoUserProvisioner = cognitoUserProvisioner;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<UserListItem> findPlatformUsers() {
        return userAdminMapper.findPlatformUsers();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public UserDetail findPlatformUserDetail(Long userId) {
        return userAdminMapper.findPlatformUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public List<UserListItem> findTenantUsers() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return userAdminMapper.findTenantUsersByCompanyId(currentUser.companyId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public UserDetail findTenantUserDetail(Long userId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return userAdminMapper.findTenantUserByIdAndCompanyId(userId, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<CompanySelectOption> findActiveCompanies() {
        return userAdminMapper.findActiveCompanies();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<DepartmentSelectOption> findActiveDepartments() {
        return userAdminMapper.findActiveDepartments();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public List<DepartmentSelectOption> findTenantActiveDepartments() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long companyId = resolveRequiredCompanyId(currentUser.companyId());
        return userAdminMapper.findActiveDepartmentsByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<PermissionSetOption> findPlatformPermissionSetOptions() {
        return userAdminMapper.findPlatformPermissionSetOptions();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN','TENANT_MANAGER')")
    public List<PermissionSetOption> findTenantPermissionSetOptions() {
        return userAdminMapper.findTenantPermissionSetOptions();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public Long createPlatformUser(UserForm userForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        if (ACTOR_TYPE_PLATFORM.equals(userForm.actorType())) {
            if (userForm.companyId() != null || userForm.departmentId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORMユーザーには会社を指定できません。");
            }
            return createUser(
                    null,
                    null,
                    ACTOR_TYPE_PLATFORM,
                    normalizePermissionSetCodes(userForm.permissionSetCodes()),
                    userForm.username(),
                    userForm.name(),
                    userForm.email(),
                    currentUser);
        }
        if (ACTOR_TYPE_TENANT.equals(userForm.actorType())) {
            Long companyId = resolveRequiredCompanyId(userForm.companyId());
            Long departmentId = resolveDepartmentId(companyId, userForm.departmentId());
            return createUser(
                    companyId,
                    departmentId,
                    ACTOR_TYPE_TENANT,
                    normalizePermissionSetCodes(userForm.permissionSetCodes()),
                    userForm.username(),
                    userForm.name(),
                    userForm.email(),
                    currentUser);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actor_typeが不正です。");
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public Long createTenantUser(UserForm userForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long companyId = resolveRequiredCompanyId(currentUser.companyId());
        Long departmentId = resolveDepartmentId(companyId, userForm.departmentId());
        return createUser(
                companyId,
                departmentId,
                ACTOR_TYPE_TENANT,
                normalizePermissionSetCodes(userForm.permissionSetCodes()),
                userForm.username(),
                userForm.name(),
                userForm.email(),
                currentUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public Long createInitialTenantManager(
            Long companyId,
            String username,
            String name,
            String email) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = resolveRequiredCompanyId(companyId);
        return createUser(
                activeCompanyId,
                null,
                ACTOR_TYPE_TENANT,
                List.of(PERMISSION_TENANT_MANAGER),
                username,
                name,
                email,
                currentUser);
    }

    private Long createUser(
            Long companyId,
            Long departmentId,
            String actorType,
            List<String> permissionSetCodes,
            String username,
            String name,
            String email,
            LoginUserContext currentUser) {
        assertPermissionSets(actorType, permissionSetCodes);
        assertUniqueUsername(companyId, username);
        assertUniqueEmail(companyId, email);

        ProvisionedCognitoUser provisionedCognitoUser = cognitoUserProvisioner.provision(
                new CognitoUserProvisionRequest(username, email));
        userAdminMapper.insertUser(
                companyId,
                departmentId,
                provisionedCognitoUser.cognitoSub(),
                username,
                name,
                email,
                actorType,
                currentUser.userId(),
                currentUser.userId());
        Long userId = userAdminMapper.findLastInsertId();
        for (String permissionSetCode : permissionSetCodes) {
            userAdminMapper.insertUserPermissionSetByCode(userId, permissionSetCode);
        }
        return userId;
    }

    private Long resolveRequiredCompanyId(Long companyId) {
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社を選択してください。");
        }
        return userAdminMapper.findActiveCompanyId(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社が見つかりません。"));
    }

    private Long resolveDepartmentId(Long companyId, Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return userAdminMapper.findActiveDepartmentIdByCompanyId(departmentId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "所属部署が見つかりません。"));
    }

    private void assertPermissionSets(String actorType, List<String> permissionSetCodes) {
        if (permissionSetCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "権限セットを選択してください。");
        }
        if (ACTOR_TYPE_PLATFORM.equals(actorType) && !PLATFORM_PERMISSION_CODES.containsAll(permissionSetCodes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORMユーザーの権限セットが不正です。");
        }
        if (ACTOR_TYPE_TENANT.equals(actorType) && !TENANT_PERMISSION_CODES.containsAll(permissionSetCodes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TENANTユーザーの権限セットが不正です。");
        }
    }

    private void assertUniqueUsername(Long companyId, String username) {
        boolean exists;
        if (companyId == null) {
            exists = userAdminMapper.existsPlatformUsername(username);
        } else {
            exists = userAdminMapper.existsTenantUsername(companyId, username);
        }
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ユーザー名は既に使用されています。");
        }
    }

    private void assertUniqueEmail(Long companyId, String email) {
        boolean exists;
        if (companyId == null) {
            exists = userAdminMapper.existsPlatformEmail(email);
        } else {
            exists = userAdminMapper.existsTenantEmail(companyId, email);
        }
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emailは既に使用されています。");
        }
    }

    private List<String> normalizePermissionSetCodes(List<String> permissionSetCodes) {
        if (permissionSetCodes == null) {
            return List.of();
        }
        return permissionSetCodes.stream()
                .distinct()
                .toList();
    }
}
