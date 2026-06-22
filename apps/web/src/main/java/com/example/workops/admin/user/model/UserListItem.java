package com.example.workops.admin.user.model;

import java.time.LocalDateTime;

/** ユーザー一覧に表示するユーザー行を表す。 */
public record UserListItem(
    Long id,
    String username,
    String name,
    String email,
    String actorType,
    String companyName,
    String departmentName,
    String permissionSetDisplay,
    LocalDateTime updatedAt) {}
