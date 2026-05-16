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
}
