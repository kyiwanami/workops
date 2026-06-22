package com.example.workops.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.workops.master.mapper.UserAccountMapper;
import com.example.workops.master.mapper.UserAccountRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** root SQLとMVP local seedがTestcontainers MySQLで成立することを確認する。 */
class SeedMigrationIntegrationTests extends MapperIntegrationTestBase {

  @Autowired private UserAccountMapper userAccountMapper;

  @Test
  void flywayAppliesMvpSchemaAndLocalSeed() {
    assertThat(countRows("companies")).isEqualTo(2L);
    assertThat(countRows("users")).isEqualTo(7L);
    assertThat(countRows("permission_sets")).isEqualTo(4L);
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
  void rootSqlAppliesLocalLocationsInVersionOrder() {
    assertThat(appliedSqlScriptNames())
        .containsExactly(
            "V1__create_mvp_schema.sql",
            "V2__allow_platform_users.sql",
            "V3__insert_demo_companies_departments.sql",
            "V4__insert_permission_sets.sql",
            "V5__insert_business_masters.sql",
            "V6__insert_users.sql",
            "V7__insert_asset_sample_seed.sql",
            "V8__insert_request_sample_seed.sql");
  }

  @Test
  void userAccountMapperFindsLocalLoginUserAndPermissionSet() {
    UserAccountRow user =
        userAccountMapper.findByCognitoSub("00000000-0000-0000-0000-000000000003").orElseThrow();

    assertThat(user.userId()).isEqualTo(3L);
    assertThat(user.companyId()).isEqualTo(1L);
    assertThat(user.actorType()).isEqualTo("TENANT");
    assertThat(userAccountMapper.findPermissionSetsByUserId(user.userId()))
        .extracting("code")
        .containsExactly("TENANT_MANAGER");
  }

  @Test
  void userAccountMapperFindsPlatformAdminLocalLoginUserAndPermissionSet() {
    UserAccountRow user =
        userAccountMapper.findByCognitoSub("00000000-0000-0000-0000-000000000000").orElseThrow();

    assertThat(user.userId()).isEqualTo(7L);
    assertThat(user.companyId()).isNull();
    assertThat(user.actorType()).isEqualTo("PLATFORM");
    assertThat(userAccountMapper.findPermissionSetsByUserId(user.userId()))
        .extracting("code")
        .containsExactly("PLATFORM_ADMIN");
  }

  private Long countByCode(String tableName, String code) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + tableName + " WHERE code = ?", Long.class, code);
  }
}
