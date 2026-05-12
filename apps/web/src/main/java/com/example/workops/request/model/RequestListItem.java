package com.example.workops.request.model;

import java.time.LocalDateTime;

/**
 * 申請一覧画面に表示する申請の要約。
 */
public record RequestListItem(
        Long id,
        String requesterName,
        Long assetId,
        String assetCode,
        String assetName,
        Boolean assetDeleted,
        String processTypeCode,
        String processTypeName,
        String statusCode,
        String statusName,
        String title,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
