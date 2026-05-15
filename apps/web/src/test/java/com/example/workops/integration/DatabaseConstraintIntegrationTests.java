package com.example.workops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MVP DB制約と削除済みコード再利用禁止をTestcontainers MySQLで確認する。
 */
class DatabaseConstraintIntegrationTests extends MapperIntegrationTestBase {

    @Test
    void companiesCodeRejectsNull() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO companies (code, name) VALUES (?, ?)",
                null,
                "M8 NULL会社"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void departmentsRejectUnknownCompanyForeignKey() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO departments (company_id, code, name) VALUES (?, ?, ?)",
                999L,
                "M8_FK",
                "M8 FK部署"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void companiesRejectDuplicateCode() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO companies (code, name) VALUES (?, ?)",
                "KTHM_PRECISION",
                "M8重複会社"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void genericMasterValuesRejectDuplicateCodeInSameMasterAndCompany() {
        assertThatThrownBy(() -> jdbcTemplate.update(
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

        assertThatThrownBy(() -> jdbcTemplate.update(
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
}
