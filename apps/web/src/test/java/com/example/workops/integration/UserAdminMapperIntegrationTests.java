package com.example.workops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.workops.admin.user.mapper.UserAdminMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ユーザー一覧・詳細SQLをTestcontainers MySQLで確認する。
 */
class UserAdminMapperIntegrationTests extends MapperIntegrationTestBase {

    @Autowired
    private UserAdminMapper userAdminMapper;

    @Test
    void platformUsersIncludePlatformAndTenantUsers() {
        assertThat(userAdminMapper.findPlatformUsers())
                .extracting("username")
                .contains("platform-admin", "kthm-manager", "aoba-manager");
    }

    @Test
    void tenantUsersIncludeOnlyOwnCompanyTenantUsers() {
        assertThat(userAdminMapper.findTenantUsersByCompanyId(1L))
                .extracting("username")
                .containsExactly("kthm-editor", "kthm-manager", "kthm-viewer");
    }

    @Test
    void tenantUserDetailRejectsOtherCompanyUser() {
        assertThat(userAdminMapper.findTenantUserByIdAndCompanyId(6L, 1L)).isEmpty();
    }

    @Test
    void tenantUserDetailRejectsPlatformUser() {
        assertThat(userAdminMapper.findTenantUserByIdAndCompanyId(7L, 1L)).isEmpty();
    }

    @Test
    void userDisplayIncludesPermissionSets() {
        assertThat(userAdminMapper.findPlatformUserById(3L))
                .hasValueSatisfying(user -> assertThat(user.permissionSetDisplay()).isEqualTo("TENANT_MANAGER / 管理者"));
    }

    @Test
    void deletedDepartmentIsDisplayedWithDeletedSuffix() {
        jdbcTemplate.update("UPDATE departments SET is_deleted = TRUE WHERE id = ?", 1L);

        assertThat(userAdminMapper.findPlatformUserById(3L))
                .hasValueSatisfying(user -> assertThat(user.departmentName()).isEqualTo("総務部（削除済み）"));
    }

    @Test
    void missingDepartmentIsDisplayedAsUnset() {
        assertThat(userAdminMapper.findPlatformUserById(7L))
                .hasValueSatisfying(user -> assertThat(user.departmentName()).isEqualTo("未設定"));
    }

    @Test
    void activeDepartmentsByCompanyIdReturnsOnlyOwnActiveDepartments() {
        jdbcTemplate.update("UPDATE departments SET is_deleted = TRUE WHERE id = ?", 1L);

        assertThat(userAdminMapper.findActiveDepartmentsByCompanyId(1L))
                .extracting("companyId")
                .containsOnly(1L);
        assertThat(userAdminMapper.findActiveDepartmentsByCompanyId(1L))
                .extracting("id")
                .doesNotContain(1L);
    }

    @Test
    void insertPlatformUserAndPermissionSet() {
        userAdminMapper.insertUser(
                null,
                null,
                "11111111-1111-1111-1111-111111111111",
                "new-platform",
                "新PLATFORM",
                "new-platform@example.local",
                "PLATFORM",
                7L,
                7L);
        Long userId = userAdminMapper.findLastInsertId();
        userAdminMapper.insertUserPermissionSetByCode(userId, "PLATFORM_ADMIN");

        assertThat(userAdminMapper.findPlatformUserById(userId))
                .hasValueSatisfying(user -> {
                    assertThat(user.actorType()).isEqualTo("PLATFORM");
                    assertThat(user.companyId()).isNull();
                    assertThat(user.permissionSetDisplay()).isEqualTo("PLATFORM_ADMIN / WorkOps管理者");
                });
    }

    @Test
    void insertTenantUserAndPermissionSet() {
        userAdminMapper.insertUser(
                1L,
                2L,
                "22222222-2222-2222-2222-222222222222",
                "new-tenant",
                "新TENANT",
                "new-tenant@example.local",
                "TENANT",
                7L,
                7L);
        Long userId = userAdminMapper.findLastInsertId();
        userAdminMapper.insertUserPermissionSetByCode(userId, "TENANT_MANAGER");

        assertThat(userAdminMapper.findTenantUserByIdAndCompanyId(userId, 1L))
                .hasValueSatisfying(user -> {
                    assertThat(user.actorType()).isEqualTo("TENANT");
                    assertThat(user.companyId()).isEqualTo(1L);
                    assertThat(user.departmentName()).isEqualTo("情報システム部");
                    assertThat(user.permissionSetDisplay()).isEqualTo("TENANT_MANAGER / 管理者");
                });
    }

    @Test
    void permissionSetOptionsAreLoadedForUserCreationForm() {
        assertThat(userAdminMapper.findPlatformPermissionSetOptions())
                .extracting("code")
                .containsExactly("PLATFORM_ADMIN");
        assertThat(userAdminMapper.findTenantPermissionSetOptions())
                .extracting("code")
                .containsExactly("TENANT_VIEWER", "TENANT_EDITOR", "TENANT_MANAGER");
    }

    @Test
    void scopedUsernameAndEmailDuplicateDetectionWorks() {
        assertThat(userAdminMapper.existsPlatformUsername("platform-admin")).isTrue();
        assertThat(userAdminMapper.existsPlatformEmail("platform-admin@example.local")).isTrue();
        assertThat(userAdminMapper.existsTenantUsername(1L, "kthm-manager")).isTrue();
        assertThat(userAdminMapper.existsTenantEmail(1L, "kthm-manager@example.local")).isTrue();
        assertThat(userAdminMapper.existsTenantUsername(2L, "kthm-manager")).isFalse();
        assertThat(userAdminMapper.existsTenantEmail(2L, "kthm-manager@example.local")).isFalse();
    }

    @Test
    void userEditTargetAndPermissionCodesAreLoaded() {
        assertThat(userAdminMapper.findPlatformUserEditTarget(3L))
                .hasValueSatisfying(user -> {
                    assertThat(user.companyId()).isEqualTo(1L);
                    assertThat(user.departmentId()).isEqualTo(1L);
                    assertThat(user.actorType()).isEqualTo("TENANT");
                    assertThat(user.cognitoSub()).isNotBlank();
                });
        assertThat(userAdminMapper.findTenantUserEditTargetByIdAndCompanyId(3L, 1L))
                .hasValueSatisfying(user -> assertThat(user.username()).isEqualTo("kthm-manager"));
        assertThat(userAdminMapper.findPermissionSetCodesByUserId(3L))
                .containsExactly("TENANT_MANAGER");
    }

    @Test
    void updateUserEditableFieldsAndReplacePermissionSets() {
        userAdminMapper.insertUser(
                1L,
                1L,
                "33333333-3333-3333-3333-333333333333",
                "edit-target",
                "編集前",
                "edit-target@example.local",
                "TENANT",
                7L,
                7L);
        Long userId = userAdminMapper.findLastInsertId();
        userAdminMapper.insertUserPermissionSetByCode(userId, "TENANT_MANAGER");

        userAdminMapper.updateUserEditableFields(
                userId,
                "編集後",
                "edit-target-updated@example.local",
                2L,
                7L);
        userAdminMapper.deleteUserPermissionSets(userId);
        userAdminMapper.insertUserPermissionSetByCode(userId, "TENANT_EDITOR");

        assertThat(userAdminMapper.findTenantUserByIdAndCompanyId(userId, 1L))
                .hasValueSatisfying(user -> {
                    assertThat(user.name()).isEqualTo("編集後");
                    assertThat(user.email()).isEqualTo("edit-target-updated@example.local");
                    assertThat(user.departmentName()).isEqualTo("情報システム部");
                    assertThat(user.permissionSetDisplay()).isEqualTo("TENANT_EDITOR / 編集者");
                });
    }

    @Test
    void emailDuplicateExcludingUserAndTenantManagerCountWork() {
        assertThat(userAdminMapper.existsTenantEmailExcludingUser(1L, 3L, "kthm-manager@example.local")).isFalse();
        assertThat(userAdminMapper.existsTenantEmailExcludingUser(1L, 3L, "kthm-editor@example.local")).isTrue();
        assertThat(userAdminMapper.existsPlatformEmailExcludingUser(7L, "platform-admin@example.local")).isFalse();

        assertThat(userAdminMapper.countActiveTenantManagersByCompanyId(1L)).isEqualTo(1);
    }
}
