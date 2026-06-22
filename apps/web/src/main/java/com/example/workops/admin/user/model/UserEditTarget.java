package com.example.workops.admin.user.model;

/** ユーザー編集画面で表示する変更不可項目と現在値。 */
public record UserEditTarget(
    Long id,
    Long companyId,
    Long departmentId,
    String username,
    String name,
    String email,
    String actorType,
    String companyName,
    String cognitoSub) {}
