package com.example.workops.master.model;

import java.time.LocalDateTime;

/**
 * 資産分類マスタ一覧画面に表示する資産分類の要約。
 */
public record AssetCategoryMasterListItem(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
