package com.example.workops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.workops.admin.department.mapper.DepartmentAdminMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 部署一覧と部署作成SQLをTestcontainers MySQLで確認する。
 */
class DepartmentAdminMapperIntegrationTests extends MapperIntegrationTestBase {

    @Autowired
    private DepartmentAdminMapper departmentAdminMapper;

    @Test
    void findActiveDepartmentsByCompanyIdExcludesDeletedRows() {
        jdbcTemplate.update("UPDATE departments SET is_deleted = TRUE WHERE id = ?", 1L);

        assertThat(departmentAdminMapper.findActiveDepartmentsByCompanyId(1L))
                .extracting("code")
                .containsExactly("IT", "MFG", "SALES");
    }

    @Test
    void existsDepartmentCodeByCompanyIdDetectsDeletedRowsToo() {
        jdbcTemplate.update("UPDATE departments SET is_deleted = TRUE WHERE id = ?", 1L);

        assertThat(departmentAdminMapper.existsDepartmentCodeByCompanyId(1L, "ADMIN")).isTrue();
    }

    @Test
    void sameDepartmentCodeCanBeCreatedInDifferentCompany() {
        departmentAdminMapper.insertDepartment(2L, "ADMIN", "青葉総務部", 7L, 7L);

        assertThat(departmentAdminMapper.existsDepartmentCodeByCompanyId(2L, "ADMIN")).isTrue();
    }

    @Test
    void insertDepartmentSetsCreatedByAndUpdatedBy() {
        departmentAdminMapper.insertDepartment(1L, "HR", "人事部", 7L, 7L);
        Long departmentId = departmentAdminMapper.findLastInsertId();

        Long createdBy = jdbcTemplate.queryForObject(
                "SELECT created_by FROM departments WHERE id = ?",
                Long.class,
                departmentId);
        Long updatedBy = jdbcTemplate.queryForObject(
                "SELECT updated_by FROM departments WHERE id = ?",
                Long.class,
                departmentId);

        assertThat(createdBy).isEqualTo(7L);
        assertThat(updatedBy).isEqualTo(7L);
    }

    @Test
    void duplicateDepartmentCodeInSameCompanyIsRejectedByDatabaseConstraint() {
        assertThatThrownBy(() -> departmentAdminMapper.insertDepartment(1L, "ADMIN", "重複総務部", 7L, 7L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
