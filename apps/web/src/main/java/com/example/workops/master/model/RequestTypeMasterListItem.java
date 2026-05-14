package com.example.workops.master.model;

import java.time.LocalDateTime;

/**
 * 申請種別マスタ一覧画面に表示する申請種別の要約。
 */
public record RequestTypeMasterListItem(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
