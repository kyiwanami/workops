package com.example.workops.common.security;

import java.util.List;

/**
 * DB由来のアプリ利用者情報をControllerやServiceへ渡す現在ユーザーコンテキスト。
 */
public record LoginUserContext(
        Long userId,
        String username,
        String email,
        String actorType,
        Long companyId,
        List<PermissionSetContext> permissionSets) {

    public LoginUserContext {
        permissionSets = List.copyOf(permissionSets);
    }
}
