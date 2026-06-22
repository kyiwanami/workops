package com.example.workops.admin.company.model;

import java.time.LocalDateTime;

/** 会社一覧に表示する会社情報と配下データ件数。 */
public record CompanyListItem(
    Long id,
    String code,
    String name,
    Boolean isDeleted,
    Long departmentCount,
    Long userCount,
    Long requestCount,
    Long assetCount,
    LocalDateTime updatedAt) {}
