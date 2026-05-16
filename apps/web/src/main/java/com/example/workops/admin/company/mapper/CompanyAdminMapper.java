package com.example.workops.admin.company.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * PLATFORM_ADMIN向け会社管理とテナント初期化のSQLを実行するMapper。
 */
@Mapper
public interface CompanyAdminMapper {

    boolean existsCompanyCode(@Param("code") String code);

    int insertCompany(
            @Param("code") String code,
            @Param("name") String name,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    Long findLastInsertId();

    Optional<Long> findActiveGenericMasterIdByCode(@Param("code") String code);

    int insertGenericMasterValue(
            @Param("genericMasterId") Long genericMasterId,
            @Param("companyId") Long companyId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("sortOrder") Integer sortOrder,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    Long countGenericMasterValuesByCompanyIdAndMasterCode(
            @Param("companyId") Long companyId,
            @Param("genericMasterCode") String genericMasterCode);
}
