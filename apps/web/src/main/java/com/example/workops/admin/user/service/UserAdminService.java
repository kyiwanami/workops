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
 * {@code PLATFORM_ADMIN} と {@code TENANT_MANAGER} 向けユーザー管理ユースケースを扱うService。
 *
 * <p>アプリ利用者情報の正本はWorkOps DBの {@code users} と {@code user_permission_sets} であり、
 * Cognitoは認証主体の作成境界として扱う。PLATFORMユーザーは会社を持たず {@code PLATFORM_ADMIN}
 * のみ、TENANTユーザーは会社に所属しTENANT系権限だけを割り当てられる。</p>
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

    /**
     * PLATFORM_ADMINが全ユーザー一覧を取得する。
     *
     * @return PLATFORMユーザーと全会社のTENANTユーザー一覧
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<UserListItem> findPlatformUsers() {
        return userAdminMapper.findPlatformUsers();
    }

    /**
     * PLATFORM_ADMINが任意ユーザーの詳細を取得する。
     *
     * @param userId ユーザーID
     * @return ユーザー詳細
     * @throws ResponseStatusException 対象ユーザーが存在しない場合
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public UserDetail findPlatformUserDetail(Long userId) {
        return userAdminMapper.findPlatformUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }

    /**
     * TENANT_MANAGERが自社TENANTユーザー一覧を取得する。
     *
     * @return 現在ユーザーの会社に属するTENANTユーザー一覧
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public List<UserListItem> findTenantUsers() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return userAdminMapper.findTenantUsersByCompanyId(currentUser.companyId());
    }

    /**
     * TENANT_MANAGERが自社TENANTユーザーの詳細を取得する。
     *
     * @param userId ユーザーID
     * @return 自社TENANTユーザー詳細
     * @throws ResponseStatusException 対象が存在しない、他社ユーザー、またはPLATFORMユーザーの場合
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public UserDetail findTenantUserDetail(Long userId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return userAdminMapper.findTenantUserByIdAndCompanyId(userId, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }

    /**
     * PLATFORM_ADMINのユーザー作成画面で選択できる未削除会社を取得する。
     *
     * @return 未削除会社の選択肢
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<CompanySelectOption> findActiveCompanies() {
        return userAdminMapper.findActiveCompanies();
    }

    /**
     * PLATFORM_ADMINのユーザー作成画面で選択できる未削除部署を取得する。
     *
     * @return 未削除部署の選択肢
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<DepartmentSelectOption> findActiveDepartments() {
        return userAdminMapper.findActiveDepartments();
    }

    /**
     * PLATFORM_ADMINが選択した会社に属する未削除部署を取得する。
     *
     * @param companyId 会社ID。未選択の場合は空リストを返す
     * @return 指定会社に属する未削除部署の選択肢
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<DepartmentSelectOption> findActiveDepartmentsByCompanyId(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return userAdminMapper.findActiveDepartmentsByCompanyId(companyId);
    }

    /**
     * TENANT_MANAGERのユーザー作成・編集画面で選択できる自社未削除部署を取得する。
     *
     * @return 現在ユーザーの会社に属する未削除部署の選択肢
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public List<DepartmentSelectOption> findTenantActiveDepartments() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long companyId = resolveRequiredCompanyId(currentUser.companyId());
        return userAdminMapper.findActiveDepartmentsByCompanyId(companyId);
    }

    /**
     * PLATFORMユーザーへ割り当て可能な権限セットを取得する。
     *
     * @return {@code PLATFORM_ADMIN} の選択肢
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<PermissionSetOption> findPlatformPermissionSetOptions() {
        return userAdminMapper.findPlatformPermissionSetOptions();
    }

    /**
     * TENANTユーザーへ割り当て可能な権限セットを取得する。
     *
     * @return {@code TENANT_VIEWER}、{@code TENANT_EDITOR}、{@code TENANT_MANAGER} の選択肢
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN','TENANT_MANAGER')")
    public List<PermissionSetOption> findTenantPermissionSetOptions() {
        return userAdminMapper.findTenantPermissionSetOptions();
    }

    /**
     * PLATFORM_ADMINがPLATFORMユーザーまたは任意会社のTENANTユーザーを作成する。
     *
     * <p>Cognitoユーザー作成境界を呼び出し、返却された {@code cognito_sub} をWorkOps DBへ登録する。</p>
     *
     * @param userForm ユーザー作成フォーム
     * @return 作成したユーザーID
     * @throws ResponseStatusException actor_type、会社、部署、権限セット、username、emailが業務条件を満たさない場合
     */
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

    /**
     * TENANT_MANAGERが自社TENANTユーザーを作成する。
     *
     * @param userForm ユーザー作成フォーム
     * @return 作成したユーザーID
     * @throws ResponseStatusException 他社部署、削除済み部署、PLATFORM権限などを指定した場合
     */
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

    /**
     * 会社作成時に初期 {@code TENANT_MANAGER} を作成する。
     *
     * @param companyId 作成先会社ID
     * @param username 初期管理者のユーザー名
     * @param name 初期管理者の表示名
     * @param email 初期管理者のメールアドレス
     * @return 作成したユーザーID
     */
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

    /**
     * PLATFORM_ADMINが編集対象ユーザーの固定属性と現在値を取得する。
     *
     * @param userId ユーザーID
     * @return 編集対象ユーザーの現在値
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public UserEditTarget findPlatformUserEditTarget(Long userId) {
        return requirePlatformUserEditTarget(userId);
    }

    /**
     * PLATFORM_ADMIN向けユーザー編集フォームの初期値を取得する。
     *
     * @param userId ユーザーID
     * @return ユーザー編集フォーム
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public UserEditForm findPlatformUserEditForm(Long userId) {
        UserEditTarget target = requirePlatformUserEditTarget(userId);
        return toEditForm(target);
    }

    /**
     * PLATFORM_ADMINがユーザーの表示名、email、部署、権限セットを更新する。
     *
     * <p>{@code actor_type}、{@code company_id}、{@code cognito_sub} は変更しない。</p>
     *
     * @param userId ユーザーID
     * @param userEditForm 更新後のユーザー編集フォーム
     * @throws ResponseStatusException 権限セット不整合、email重複、TENANT_MANAGER 0人化が発生する場合
     */
    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void updatePlatformUser(Long userId, UserEditForm userEditForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        UserEditTarget target = requirePlatformUserEditTarget(userId);
        updateUser(target, userEditForm, currentUser, null);
    }

    /**
     * TENANT_MANAGERが自社編集対象ユーザーの固定属性と現在値を取得する。
     *
     * @param userId ユーザーID
     * @return 自社TENANTユーザーの現在値
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public UserEditTarget findTenantUserEditTarget(Long userId) {
        return requireTenantUserEditTarget(userId);
    }

    /**
     * TENANT_MANAGER向け自社ユーザー編集フォームの初期値を取得する。
     *
     * @param userId ユーザーID
     * @return ユーザー編集フォーム
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public UserEditForm findTenantUserEditForm(Long userId) {
        UserEditTarget target = requireTenantUserEditTarget(userId);
        return toEditForm(target);
    }

    /**
     * TENANT_MANAGERが自社TENANTユーザーの表示名、email、部署、権限セットを更新する。
     *
     * <p>最終UPDATEでも現在ユーザーの会社IDをSQL条件に含め、他社ユーザーやPLATFORMユーザーは更新しない。</p>
     *
     * @param userId ユーザーID
     * @param userEditForm 更新後のユーザー編集フォーム
     * @throws ResponseStatusException 他社ユーザー、PLATFORMユーザー、権限セット不整合、email重複、
     *         TENANT_MANAGER 0人化が発生する場合
     */
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
