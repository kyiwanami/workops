package com.example.workops.asset.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.asset.model.AssetDetail;
import com.example.workops.asset.model.AssetListItem;

/**
 * 資産カタログの参照系SQLを実行するMapper。
 */
@Mapper
public interface AssetMapper {

    List<AssetListItem> findListByCompanyId(@Param("companyId") Long companyId);

    Optional<AssetDetail> findDetailByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId);
}
