package com.example.workops.admin.department.model;

import java.time.LocalDateTime;

/** 部署一覧に表示する部署行を表す。 */
public record DepartmentListItem(
    Long id,
    String code,
    String name,
    Boolean isDeleted,
    Long activeUserCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
