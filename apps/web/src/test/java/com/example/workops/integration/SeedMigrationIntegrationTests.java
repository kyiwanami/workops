package com.example.workops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.workops.master.mapper.UserAccountMapper;
import com.example.workops.master.mapper.UserAccountRow;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway migrationとMVP local seedがTestcontainers MySQLで成立することを確認する。
 */
class SeedMigrationIntegrationTests extends MapperIntegrationTestBase {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Test
    void flywayAppliesMvpSchemaAndLocalSeed() {
        assertThat(countRows("companies")).isEqualTo(2L);
        assertThat(countRows("users")).isEqualTo(6L);
        assertThat(countRows("permission_sets")).isEqualTo(3L);
        assertThat(countRows("common_master")).isEqualTo(2L);
        assertThat(countRows("generic_master")).isEqualTo(2L);

        Long requestStatusCount = countByCode("common_master", "REQUEST_STATUS");
        Long assetStatusCount = countByCode("common_master", "ASSET_STATUS");
        Long assetCategoryCount = countByCode("generic_master", "ASSET_CATEGORY");
        Long requestTypeCount = countByCode("generic_master", "REQUEST_TYPE");

        assertThat(requestStatusCount).isEqualTo(1L);
        assertThat(assetStatusCount).isEqualTo(1L);
        assertThat(assetCategoryCount).isEqualTo(1L);
        assertThat(requestTypeCount).isEqualTo(1L);
    }

    @Test
    void userAccountMapperFindsLocalLoginUserAndPermissionSet() {
        UserAccountRow user = userAccountMapper
                .findByCognitoSub("00000000-0000-0000-0000-000000000003")
                .orElseThrow();

        assertThat(user.userId()).isEqualTo(3L);
        assertThat(user.companyId()).isEqualTo(1L);
        assertThat(user.actorType()).isEqualTo("TENANT");
        assertThat(userAccountMapper.findPermissionSetsByUserId(user.userId()))
                .extracting("code")
                .containsExactly("TENANT_MANAGER");
    }

    private Long countByCode(String tableName, String code) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE code = ?",
                Long.class,
                code);
    }
}
