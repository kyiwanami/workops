package com.example.workops.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

/** MVP DB制約と削除済みコード再利用禁止をTestcontainers MySQLで確認する。 */
class DatabaseConstraintIntegrationTests extends MapperIntegrationTestBase {

  @Test
  void companiesCodeRejectsNull() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO companies (code, name) VALUES (?, ?)", null, "M8 NULL会社"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void departmentsRejectUnknownCompanyForeignKey() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO departments (company_id, code, name) VALUES (?, ?, ?)",
                    999L,
                    "M8_FK",
                    "M8 FK部署"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void companiesRejectDuplicateCode() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO companies (code, name) VALUES (?, ?)", "KTHM_PRECISION", "M8重複会社"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void genericMasterValuesRejectDuplicateCodeInSameMasterAndCompany() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                INSERT INTO generic_master_values (
                    generic_master_id,
                    company_id,
                    code,
                    name,
                    sort_order
                ) VALUES (?, ?, ?, ?, ?)
                """,
                    1L,
                    1L,
                    "NOTE_PC",
                    "M8重複資産分類",
                    999))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void logicalDeletedGenericMasterValueCodeCannotBeReused() {
    jdbcTemplate.update("UPDATE generic_master_values SET is_deleted = TRUE WHERE id = ?", 6L);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                INSERT INTO generic_master_values (
                    generic_master_id,
                    company_id,
                    code,
                    name,
                    sort_order
                ) VALUES (?, ?, ?, ?, ?)
                """,
                    1L,
                    1L,
                    "OTHER",
                    "M8削除済みコード再利用",
                    999))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void tenantUserRejectsNullCompanyId() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                INSERT INTO users (
                    company_id,
                    cognito_sub,
                    username,
                    name,
                    email,
                    actor_type
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                    null,
                    "10000000-0000-0000-0000-000000000001",
                    "m9-tenant-null-company",
                    "M9 テナント会社なし",
                    "m9-tenant-null-company@example.local",
                    "TENANT"))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void platformUserRejectsCompanyId() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                INSERT INTO users (
                    company_id,
                    cognito_sub,
                    username,
                    name,
                    email,
                    actor_type
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                    1L,
                    "10000000-0000-0000-0000-000000000002",
                    "m9-platform-with-company",
                    "M9 会社ありPLATFORM",
                    "m9-platform-with-company@example.local",
                    "PLATFORM"))
        .isInstanceOf(DataAccessException.class);
  }
}
