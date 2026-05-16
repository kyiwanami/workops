package com.example.workops.admin.department.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.admin.department.model.DepartmentListItem;

/**
 * 部署一覧と部署作成のSQLを実行するMapper。
 */
@Mapper
public interface DepartmentAdminMapper {

    Optional<Long> findActiveCompanyId(@Param("companyId") Long companyId);

    Optional<String> findActiveCompanyCodeById(@Param("companyId") Long companyId);

    Optional<String> findActiveCompanyNameById(@Param("companyId") Long companyId);

    List<DepartmentListItem> findActiveDepartmentsByCompanyId(@Param("companyId") Long companyId);

    boolean existsDepartmentCodeByCompanyId(
            @Param("companyId") Long companyId,
            @Param("code") String code);

    int insertDepartment(
            @Param("companyId") Long companyId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    Long findLastInsertId();
}
