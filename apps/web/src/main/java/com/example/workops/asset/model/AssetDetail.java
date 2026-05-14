package com.example.workops.asset.model;

import java.time.LocalDateTime;

/**
 * 資産詳細画面に表示する資産の現在状態。
 */
public record AssetDetail(
        Long id,
        Long assetCategoryValueId,
        Long departmentId,
        String code,
        String name,
        String assetCategoryCode,
        String assetCategoryName,
        Boolean assetCategoryValueIsDeleted,
        String departmentName,
        String statusCode,
        String statusName,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
