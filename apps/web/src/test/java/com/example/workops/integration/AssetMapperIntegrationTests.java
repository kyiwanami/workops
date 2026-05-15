package com.example.workops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.workops.asset.form.AssetSearchForm;
import com.example.workops.asset.mapper.AssetMapper;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.master.mapper.AssetCategoryMasterMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 資産Mapperの主要検索、更新、会社境界、論理削除済み参照先の扱いを実DBで確認する。
 */
class AssetMapperIntegrationTests extends MapperIntegrationTestBase {

    @Autowired
    private AssetMapper assetMapper;

    @Autowired
    private AssetCategoryMasterMapper assetCategoryMasterMapper;

    @Test
    void assetMapperCreatesFindsFiltersAndUpdatesAssetInsideCompanyBoundary() {
        Long assetId = createAsset(1L, "KTHM-M8-ASSET", "M8資産");

        assertThat(assetMapper.findListByCompanyIdAndSearchForm(1L, searchByCode("M8-ASSET")))
                .anySatisfy(item -> {
                    assertThat(item.id()).isEqualTo(assetId);
                    assertThat(item.assetCategoryCode()).isEqualTo("NOTE_PC");
                    assertThat(item.statusCode()).isEqualTo("AVAILABLE");
                });
        assertThat(assetMapper.findListByCompanyIdAndSearchForm(2L, searchByCode("M8-ASSET")))
                .isEmpty();

        AssetDetail detail = assetMapper.findDetailByIdAndCompanyId(assetId, 1L).orElseThrow();
        assertThat(detail.name()).isEqualTo("M8資産");
        assertThat(assetMapper.findDetailByIdAndCompanyId(assetId, 2L)).isEmpty();

        int otherCompanyUpdated = assetMapper.updateAssetStatusByIdAndCompanyId(assetId, 2L, "REPAIRING", 2L);
        int updated = assetMapper.updateAssetStatusByIdAndCompanyId(assetId, 1L, "REPAIRING", 2L);

        assertThat(otherCompanyUpdated).isZero();
        assertThat(updated).isEqualTo(1);
        assertThat(assetMapper.findDetailByIdAndCompanyId(assetId, 1L).orElseThrow().statusCode())
                .isEqualTo("REPAIRING");
    }

    @Test
    void assetMapperLogicalDeleteHidesAssetFromListAndDetail() {
        Long assetId = createAsset(1L, "KTHM-M8-DELETE", "M8論理削除資産");

        int otherCompanyDeleted = assetMapper.logicalDeleteAssetByIdAndCompanyId(assetId, 2L, 3L);
        int deleted = assetMapper.logicalDeleteAssetByIdAndCompanyId(assetId, 1L, 3L);

        assertThat(otherCompanyDeleted).isZero();
        assertThat(deleted).isEqualTo(1);
        assertThat(assetMapper.findDetailByIdAndCompanyId(assetId, 1L)).isEmpty();
        assertThat(assetMapper.findListByCompanyIdAndSearchForm(1L, searchByCode("M8-DELETE"))).isEmpty();
    }

    @Test
    void assetMapperShowsParentAssetWhenAssetCategoryIsDeletedAndExcludesItFromOptions() {
        Long assetId = createAsset(2L, "KTHM-M8-DELETED-CATEGORY", "M8削除済み分類資産");

        assetCategoryMasterMapper.logicalDeleteActiveByIdAndCompanyId(2L, 1L, 3L);

        AssetDetail detail = assetMapper.findDetailByIdAndCompanyId(assetId, 1L).orElseThrow();
        assertThat(detail.assetCategoryValueId()).isEqualTo(2L);
        assertThat(detail.assetCategoryValueIsDeleted()).isTrue();
        assertThat(assetMapper.findAssetCategoryOptionsByCompanyId(1L))
                .noneSatisfy(option -> assertThat(option.id()).isEqualTo(2L));
        assertThat(assetMapper.existsAssetCategoryByIdAndCompanyId(2L, 1L)).isFalse();
    }

    private Long createAsset(Long assetCategoryValueId, String code, String name) {
        assetMapper.insertAsset(1L, assetCategoryValueId, 2L, code, name, "AVAILABLE", "M8備考", 2L, 2L);
        return assetMapper.findLastInsertId();
    }

    private AssetSearchForm searchByCode(String code) {
        return new AssetSearchForm(code, null, null, null, null);
    }
}
