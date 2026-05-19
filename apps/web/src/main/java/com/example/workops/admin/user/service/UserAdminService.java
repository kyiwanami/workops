package com.example.workops.admin.user.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.admin.user.form.UserEditForm;
import com.example.workops.admin.user.form.UserForm;
import com.example.workops.admin.user.mapper.UserAdminMapper;
import com.example.workops.admin.user.model.CompanySelectOption;
import com.example.workops.admin.user.model.DepartmentSelectOption;
import com.example.workops.admin.user.model.PermissionSetOption;
import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserEditTarget;
import com.example.workops.admin.user.model.UserListItem;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetCode;

/**
 * PLATFORM_ADMINとTENANT_MANAGER向けユーザー参照ユースケースを扱うService。
 */
@Service
public class UserAdminService {

    private static final String ACTOR_TYPE_PLATFORM = "PLATFORM";
    private static final String ACTOR_TYPE_TENANT = "TENANT";

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
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<DepartmentSelectOption> findActiveDepartmentsByCompanyId(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return userAdminMapper.findActiveDepartmentsByCompanyId(companyId);
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
                List.of(PermissionSetCode.TENANT_MANAGER.name()),
                username,
                name,
                email,
                currentUser);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public UserEditTarget findPlatformUserEditTarget(Long userId) {
        return requirePlatformUserEditTarget(userId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public UserEditForm findPlatformUserEditForm(Long userId) {
        UserEditTarget target = requirePlatformUserEditTarget(userId);
        return toEditForm(target);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void updatePlatformUser(Long userId, UserEditForm userEditForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        UserEditTarget target = requirePlatformUserEditTarget(userId);
        updateUser(target, userEditForm, currentUser, null);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public UserEditTarget findTenantUserEditTarget(Long userId) {
        return requireTenantUserEditTarget(userId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public UserEditForm findTenantUserEditForm(Long userId) {
        UserEditTarget target = requireTenantUserEditTarget(userId);
        return toEditForm(target);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void updateTenantUser(Long userId, UserEditForm userEditForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        UserEditTarget target = requireTenantUserEditTarget(userId);
        updateUser(target, userEditForm, currentUser, currentUser.companyId());
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

    private UserEditTarget requirePlatformUserEditTarget(Long userId) {
        return userAdminMapper.findPlatformUserEditTarget(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }

    private UserEditTarget requireTenantUserEditTarget(Long userId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(userId, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }

    private UserEditForm toEditForm(UserEditTarget target) {
        return new UserEditForm(
                target.name(),
                target.email(),
                target.departmentId(),
                userAdminMapper.findPermissionSetCodesByUserId(target.id()));
    }

    private void updateUser(
            UserEditTarget target,
            UserEditForm userEditForm,
            LoginUserContext currentUser,
            Long tenantCompanyId) {
        List<String> permissionSetCodes = normalizePermissionSetCodes(userEditForm.permissionSetCodes());
        assertPermissionSets(target.actorType(), permissionSetCodes);
        Long departmentId = resolveEditDepartmentId(target, userEditForm.departmentId());
        assertUniqueEmailExcludingUser(target.companyId(), target.id(), userEditForm.email());

        updateUserEditableFields(target, userEditForm, currentUser, departmentId, tenantCompanyId);
        userAdminMapper.deleteUserPermissionSets(target.id());
        for (String permissionSetCode : permissionSetCodes) {
            userAdminMapper.insertUserPermissionSetByCode(target.id(), permissionSetCode);
        }
        if (ACTOR_TYPE_TENANT.equals(target.actorType())
                && userAdminMapper.countActiveTenantManagersByCompanyId(target.companyId()) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TENANT_MANAGERは最低1人必要です。");
        }
    }

    private void updateUserEditableFields(
            UserEditTarget target,
            UserEditForm userEditForm,
            LoginUserContext currentUser,
            Long departmentId,
            Long tenantCompanyId) {
        // TENANT導線では最終UPDATEでも現在ユーザーの会社境界をSQL条件に含める。
        if (tenantCompanyId == null) {
            userAdminMapper.updatePlatformUserEditableFields(
                    target.id(),
                    userEditForm.name(),
                    userEditForm.email(),
                    departmentId,
                    currentUser.userId());
            return;
        }

        userAdminMapper.updateTenantUserEditableFields(
                target.id(),
                tenantCompanyId,
                userEditForm.name(),
                userEditForm.email(),
                departmentId,
                currentUser.userId());
    }

    private Long resolveEditDepartmentId(UserEditTarget target, Long departmentId) {
        if (ACTOR_TYPE_PLATFORM.equals(target.actorType())) {
            if (departmentId != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORMユーザーには部署を指定できません。");
            }
            return null;
        }
        return resolveDepartmentId(target.companyId(), departmentId);
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
        if (ACTOR_TYPE_PLATFORM.equals(actorType) && !PermissionSetCode.isValidPlatformCodes(permissionSetCodes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORMユーザーの権限セットが不正です。");
        }
        if (ACTOR_TYPE_TENANT.equals(actorType) && !PermissionSetCode.isValidTenantCodes(permissionSetCodes)) {
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

    private void assertUniqueEmailExcludingUser(Long companyId, Long userId, String email) {
        boolean exists;
        if (companyId == null) {
            exists = userAdminMapper.existsPlatformEmailExcludingUser(userId, email);
        } else {
            exists = userAdminMapper.existsTenantEmailExcludingUser(companyId, userId, email);
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
