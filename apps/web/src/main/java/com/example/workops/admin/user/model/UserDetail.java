package com.example.workops.admin.user.model;

import java.time.LocalDateTime;

/**
 * ユーザー詳細画面に表示するユーザー情報を表す。
 */
public record UserDetail(
        Long id,
        Long companyId,
        String username,
        String name,
        String email,
        String actorType,
        String companyName,
        String departmentName,
        String permissionSetDisplay,
        String cognitoSub,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
