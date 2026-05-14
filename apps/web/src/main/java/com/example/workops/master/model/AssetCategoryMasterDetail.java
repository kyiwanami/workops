package com.example.workops.master.model;

import java.time.LocalDateTime;

/**
 * 資産分類マスタ編集操作で扱う資産分類の詳細。
 */
public record AssetCategoryMasterDetail(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
