package com.example.workops.admin.company.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.admin.company.form.CompanySearchForm;
import com.example.workops.admin.company.model.CompanyDetail;
import com.example.workops.admin.company.model.CompanyListItem;

/**
 * PLATFORM_ADMIN向け会社管理とテナント初期化のSQLを実行するMapper。
 */
@Mapper
public interface CompanyAdminMapper {

    List<CompanyListItem> findCompaniesBySearchForm(
            @Param("companySearchForm") CompanySearchForm companySearchForm);

    Optional<CompanyDetail> findCompanyDetailById(@Param("companyId") Long companyId);

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
