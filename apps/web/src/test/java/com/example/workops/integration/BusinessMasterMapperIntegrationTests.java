package com.example.workops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.workops.master.form.AssetCategoryMasterSearchForm;
import com.example.workops.master.form.RequestTypeMasterSearchForm;
import com.example.workops.master.mapper.AssetCategoryMasterMapper;
import com.example.workops.master.mapper.RequestTypeMasterMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会社別汎用マスタMapperの一覧、更新、会社境界、論理削除・復活を実DBで確認する。
 */
class BusinessMasterMapperIntegrationTests extends MapperIntegrationTestBase {

    @Autowired
    private RequestTypeMasterMapper requestTypeMasterMapper;

    @Autowired
    private AssetCategoryMasterMapper assetCategoryMasterMapper;

    @Test
    void requestTypeMasterMapperHandlesCrudAndCompanyBoundary() {
        Long masterId = requestTypeMasterMapper.findRequestTypeMasterId().orElseThrow();
        requestTypeMasterMapper.insertRequestType(masterId, 1L, "M8_REQUEST_TYPE", "M8申請種別", 90, 3L, 3L);
        Long requestTypeId = requestTypeMasterMapper.findLastInsertId();

        assertThat(requestTypeMasterMapper.existsCodeByCompanyId(1L, "M8_REQUEST_TYPE")).isTrue();
        assertThat(requestTypeMasterMapper.existsCodeByCompanyId(2L, "M8_REQUEST_TYPE")).isFalse();
        assertThat(requestTypeMasterMapper.findActiveByIdAndCompanyId(requestTypeId, 1L)).isPresent();
        assertThat(requestTypeMasterMapper.findActiveByIdAndCompanyId(requestTypeId, 2L)).isEmpty();

        int otherCompanyUpdated = requestTypeMasterMapper.updateActiveByIdAndCompanyId(
                requestTypeId,
                2L,
                "他社更新",
                91,
                3L);
        int updated = requestTypeMasterMapper.updateActiveByIdAndCompanyId(
                requestTypeId,
                1L,
                "M8申請種別更新",
                91,
                3L);

        assertThat(otherCompanyUpdated).isZero();
        assertThat(updated).isEqualTo(1);

        requestTypeMasterMapper.logicalDeleteActiveByIdAndCompanyId(requestTypeId, 1L, 3L);

        assertThat(requestTypeMasterMapper.findActiveByIdAndCompanyId(requestTypeId, 1L)).isEmpty();
        assertThat(requestTypeMasterMapper.findDeletedByIdAndCompanyId(requestTypeId, 1L)).isPresent();
        assertThat(requestTypeMasterMapper.findListByCompanyIdAndSearchForm(
                1L,
                new RequestTypeMasterSearchForm(false)))
                .noneSatisfy(item -> assertThat(item.id()).isEqualTo(requestTypeId));
        assertThat(requestTypeMasterMapper.findListByCompanyIdAndSearchForm(
                1L,
                new RequestTypeMasterSearchForm(true)))
                .anySatisfy(item -> assertThat(item.id()).isEqualTo(requestTypeId));
        assertThat(requestTypeMasterMapper.existsCodeByCompanyId(1L, "M8_REQUEST_TYPE")).isTrue();

        requestTypeMasterMapper.restoreDeletedByIdAndCompanyId(requestTypeId, 1L, 3L);

        assertThat(requestTypeMasterMapper.findActiveByIdAndCompanyId(requestTypeId, 1L)).isPresent();
    }

    @Test
    void assetCategoryMasterMapperHandlesCrudAndCompanyBoundary() {
        Long masterId = assetCategoryMasterMapper.findAssetCategoryMasterId().orElseThrow();
        assetCategoryMasterMapper.insertAssetCategory(masterId, 1L, "M8_ASSET_CATEGORY", "M8資産分類", 90, 3L, 3L);
        Long assetCategoryId = assetCategoryMasterMapper.findLastInsertId();

        assertThat(assetCategoryMasterMapper.existsCodeByCompanyId(1L, "M8_ASSET_CATEGORY")).isTrue();
        assertThat(assetCategoryMasterMapper.existsCodeByCompanyId(2L, "M8_ASSET_CATEGORY")).isFalse();
        assertThat(assetCategoryMasterMapper.findActiveByIdAndCompanyId(assetCategoryId, 1L)).isPresent();
        assertThat(assetCategoryMasterMapper.findActiveByIdAndCompanyId(assetCategoryId, 2L)).isEmpty();

        int otherCompanyDeleted = assetCategoryMasterMapper.logicalDeleteActiveByIdAndCompanyId(
                assetCategoryId,
                2L,
                3L);
        int deleted = assetCategoryMasterMapper.logicalDeleteActiveByIdAndCompanyId(
                assetCategoryId,
                1L,
                3L);

        assertThat(otherCompanyDeleted).isZero();
        assertThat(deleted).isEqualTo(1);
        assertThat(assetCategoryMasterMapper.findActiveByIdAndCompanyId(assetCategoryId, 1L)).isEmpty();
        assertThat(assetCategoryMasterMapper.findDeletedByIdAndCompanyId(assetCategoryId, 1L)).isPresent();
        assertThat(assetCategoryMasterMapper.findListByCompanyIdAndSearchForm(
                1L,
                new AssetCategoryMasterSearchForm(false)))
                .noneSatisfy(item -> assertThat(item.id()).isEqualTo(assetCategoryId));
        assertThat(assetCategoryMasterMapper.findListByCompanyIdAndSearchForm(
                1L,
                new AssetCategoryMasterSearchForm(true)))
                .anySatisfy(item -> assertThat(item.id()).isEqualTo(assetCategoryId));
        assertThat(assetCategoryMasterMapper.existsCodeByCompanyId(1L, "M8_ASSET_CATEGORY")).isTrue();

        assetCategoryMasterMapper.restoreDeletedByIdAndCompanyId(assetCategoryId, 1L, 3L);

        assertThat(assetCategoryMasterMapper.findActiveByIdAndCompanyId(assetCategoryId, 1L)).isPresent();
    }
}
