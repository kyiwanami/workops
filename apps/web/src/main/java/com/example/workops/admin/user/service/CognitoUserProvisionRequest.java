package com.example.workops.admin.user.service;

/**
 * Cognitoユーザー作成に渡す最小入力。
 *
 * @param username Cognitoのusernameとして渡すWorkOpsユーザー名
 * @param email Cognitoのemail属性として渡すメールアドレス
 */
public record CognitoUserProvisionRequest(
        String username,
        String email) {
}
