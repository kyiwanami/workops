package com.example.workops.asset.model;

import java.time.LocalDateTime;

/**
 * 資産一覧画面に表示する資産の要約。
 */
public record AssetListItem(
        Long id,
        String code,
        String name,
        String assetCategoryCode,
        String assetCategoryName,
        String departmentName,
        String statusCode,
        String statusName,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
