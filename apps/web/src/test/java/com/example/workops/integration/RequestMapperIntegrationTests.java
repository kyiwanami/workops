package com.example.workops.integration;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.workops.asset.mapper.AssetMapper;
import com.example.workops.master.mapper.RequestTypeMasterMapper;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 申請Mapperの主要検索、更新、会社境界、論理削除済み参照先の扱いを実DBで確認する。
 */
class RequestMapperIntegrationTests extends MapperIntegrationTestBase {

    @Autowired
    private RequestMapper requestMapper;

    @Autowired
    private AssetMapper assetMapper;

    @Autowired
    private RequestTypeMasterMapper requestTypeMasterMapper;

    @Test
    void requestMapperCreatesFindsAndSubmitsRequestInsideCompanyBoundary() {
        Long assetId = createAsset("KTHM-M8-REQ-ASSET");
        requestMapper.insertDraft(
                1L,
                2L,
                assetId,
                10L,
                "M8申請",
                "M8申請内容",
                2L,
                2L);
        Long requestId = requestMapper.findLastInsertId();

        assertThat(requestMapper.findListByCompanyId(1L))
                .anySatisfy(item -> {
                    assertThat(item.id()).isEqualTo(requestId);
                    assertThat(item.assetId()).isEqualTo(assetId);
                    assertThat(item.requestTypeCode()).isEqualTo("EQUIPMENT_PURCHASE");
                    assertThat(item.statusCode()).isEqualTo("DRAFT");
                });
        assertThat(requestMapper.findListByCompanyId(2L))
                .noneSatisfy(item -> assertThat(item.id()).isEqualTo(requestId));

        RequestDetail detail = requestMapper.findDetailByIdAndCompanyId(requestId, 1L).orElseThrow();
        assertThat(detail.title()).isEqualTo("M8申請");
        assertThat(requestMapper.findDetailByIdAndCompanyId(requestId, 2L)).isEmpty();

        int otherCompanyUpdated = requestMapper.submitDraftByIdAndCompanyId(
                requestId,
                2L,
                2L,
                LocalDateTime.now(),
                2L);
        int updated = requestMapper.submitDraftByIdAndCompanyId(
                requestId,
                1L,
                2L,
                LocalDateTime.now(),
                2L);

        assertThat(otherCompanyUpdated).isZero();
        assertThat(updated).isEqualTo(1);
        assertThat(requestMapper.findDetailByIdAndCompanyId(requestId, 1L).orElseThrow().statusCode())
                .isEqualTo("SUBMITTED");
    }

    @Test
    void requestMapperShowsParentRequestWhenLinkedAssetIsDeletedAndExcludesItFromOptions() {
        Long assetId = createAsset("KTHM-M8-DELETED-ASSET");
        requestMapper.insertDraft(
                1L,
                2L,
                assetId,
                10L,
                "削除済み資産参照申請",
                "削除済み資産参照申請内容",
                2L,
                2L);
        Long requestId = requestMapper.findLastInsertId();

        assetMapper.logicalDeleteAssetByIdAndCompanyId(assetId, 1L, 3L);

        RequestDetail detail = requestMapper.findDetailByIdAndCompanyId(requestId, 1L).orElseThrow();
        assertThat(detail.assetId()).isEqualTo(assetId);
        assertThat(detail.assetDeleted()).isTrue();
        assertThat(requestMapper.findAssetOptionsByCompanyId(1L))
                .noneSatisfy(option -> assertThat(option.id()).isEqualTo(assetId));
        assertThat(requestMapper.existsSelectableAssetByIdAndCompanyId(assetId, 1L)).isFalse();
    }

    @Test
    void requestMapperShowsParentRequestWhenRequestTypeIsDeletedAndExcludesItFromOptions() {
        requestMapper.insertDraft(
                1L,
                2L,
                null,
                11L,
                "削除済み申請種別参照申請",
                "削除済み申請種別参照申請内容",
                2L,
                2L);
        Long requestId = requestMapper.findLastInsertId();

        requestTypeMasterMapper.logicalDeleteActiveByIdAndCompanyId(11L, 1L, 3L);

        RequestDetail detail = requestMapper.findDetailByIdAndCompanyId(requestId, 1L).orElseThrow();
        assertThat(detail.requestTypeValueId()).isEqualTo(11L);
        assertThat(detail.requestTypeValueIsDeleted()).isTrue();
        assertThat(requestMapper.findRequestTypeOptionsByCompanyId(1L))
                .noneSatisfy(option -> assertThat(option.id()).isEqualTo(11L));
        assertThat(requestMapper.existsRequestTypeByIdAndCompanyId(11L, 1L)).isFalse();
    }

    private Long createAsset(String code) {
        assetMapper.insertAsset(1L, 1L, 2L, code, "M8申請紐づけ資産", "AVAILABLE", null, 2L, 2L);
        return assetMapper.findLastInsertId();
    }
}
