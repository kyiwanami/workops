package com.example.workops.master.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.master.form.AssetCategoryMasterSearchForm;
import com.example.workops.master.model.AssetCategoryMasterDetail;
import com.example.workops.master.model.AssetCategoryMasterListItem;

/**
 * 資産分類マスタ管理のSQLを実行するMapper。
 */
@Mapper
public interface AssetCategoryMasterMapper {

    Optional<Long> findAssetCategoryMasterId();

    Long findLastInsertId();

    List<AssetCategoryMasterListItem> findListByCompanyIdAndSearchForm(
            @Param("companyId") Long companyId,
            @Param("assetCategoryMasterSearchForm") AssetCategoryMasterSearchForm assetCategoryMasterSearchForm);

    Optional<AssetCategoryMasterDetail> findActiveByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    Optional<AssetCategoryMasterDetail> findDeletedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    boolean existsCodeByCompanyId(
            @Param("companyId") Long companyId,
            @Param("code") String code);

    int insertAssetCategory(
            @Param("genericMasterId") Long genericMasterId,
            @Param("companyId") Long companyId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("sortOrder") Integer sortOrder,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    int updateActiveByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("name") String name,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") Long updatedBy);

    int logicalDeleteActiveByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("updatedBy") Long updatedBy);

    int restoreDeletedByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("updatedBy") Long updatedBy);
}
