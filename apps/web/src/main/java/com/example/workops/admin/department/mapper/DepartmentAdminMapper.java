package com.example.workops.admin.department.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.admin.department.form.DepartmentSearchForm;
import com.example.workops.admin.department.model.DepartmentListItem;

/**
 * 部署一覧と部署作成のSQLを実行するMapper。
 */
@Mapper
public interface DepartmentAdminMapper {

    Optional<Long> findActiveCompanyId(@Param("companyId") Long companyId);

    Optional<String> findActiveCompanyCodeById(@Param("companyId") Long companyId);

    Optional<String> findActiveCompanyNameById(@Param("companyId") Long companyId);

    List<DepartmentListItem> findDepartmentsByCompanyIdAndSearchForm(
            @Param("companyId") Long companyId,
            @Param("departmentSearchForm") DepartmentSearchForm departmentSearchForm);

    Optional<DepartmentListItem> findActiveDepartmentByIdAndCompanyId(
            @Param("departmentId") Long departmentId,
            @Param("companyId") Long companyId);

    boolean existsDepartmentCodeByCompanyId(
            @Param("companyId") Long companyId,
            @Param("code") String code);

    int insertDepartment(
            @Param("companyId") Long companyId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    int updateActiveDepartmentNameByIdAndCompanyId(
            @Param("departmentId") Long departmentId,
            @Param("companyId") Long companyId,
            @Param("name") String name,
            @Param("updatedBy") Long updatedBy);

    int logicalDeleteActiveDepartmentByIdAndCompanyId(
            @Param("departmentId") Long departmentId,
            @Param("companyId") Long companyId,
            @Param("updatedBy") Long updatedBy);

    Long findLastInsertId();
}
