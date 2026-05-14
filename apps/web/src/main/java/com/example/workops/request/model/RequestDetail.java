package com.example.workops.request.model;

import java.time.LocalDateTime;

/**
 * 申請詳細画面に表示する申請の現在状態。
 */
public record RequestDetail(
        Long id,
        Long requesterUserId,
        Long assetId,
        String assetCode,
        String assetName,
        Boolean assetDeleted,
        String requesterName,
        Long requestTypeValueId,
        String requestTypeCode,
        String requestTypeName,
        Boolean requestTypeValueIsDeleted,
        String statusCode,
        String statusName,
        String title,
        String content,
        String reviewComment,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
