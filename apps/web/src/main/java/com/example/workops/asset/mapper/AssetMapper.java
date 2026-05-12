package com.example.workops.asset.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.asset.model.AssetDetail;
import com.example.workops.asset.model.AssetListItem;
import com.example.workops.asset.model.AssetCategoryOption;
import com.example.workops.asset.model.AssetDepartmentOption;
import com.example.workops.asset.model.AssetStatusOption;

/**
 * 資産カタログの参照系SQLを実行するMapper。
 */
@Mapper
public interface AssetMapper {

    List<AssetListItem> findListByCompanyId(@Param("companyId") Long companyId);

    Optional<AssetDetail> findDetailByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    List<AssetCategoryOption> findAssetCategoryOptionsByCompanyId(@Param("companyId") Long companyId);

    List<AssetDepartmentOption> findDepartmentOptionsByCompanyId(@Param("companyId") Long companyId);

    List<AssetStatusOption> findStatusOptions();

    boolean existsAssetCategoryByIdAndCompanyId(
            @Param("assetCategoryValueId") Long assetCategoryValueId,
            @Param("companyId") Long companyId);

    boolean existsDepartmentByIdAndCompanyId(
            @Param("departmentId") Long departmentId,
            @Param("companyId") Long companyId);

    boolean existsStatusCode(@Param("statusCode") String statusCode);

    boolean existsAssetCodeByCompanyId(
            @Param("code") String code,
            @Param("companyId") Long companyId);

    boolean existsOtherAssetCodeByCompanyId(
            @Param("id") Long id,
            @Param("code") String code,
            @Param("companyId") Long companyId);

    int insertAsset(
            @Param("companyId") Long companyId,
            @Param("assetCategoryValueId") Long assetCategoryValueId,
            @Param("departmentId") Long departmentId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("statusCode") String statusCode,
            @Param("note") String note,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    Long findLastInsertId();

    int updateAssetByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("assetCategoryValueId") Long assetCategoryValueId,
            @Param("departmentId") Long departmentId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("statusCode") String statusCode,
            @Param("note") String note,
            @Param("updatedBy") Long updatedBy);
}
