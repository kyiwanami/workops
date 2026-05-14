package com.example.workops.master.model;

import java.time.LocalDateTime;

/**
 * 申請種別マスタ編集操作で扱う申請種別の詳細。
 */
public record RequestTypeMasterDetail(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
