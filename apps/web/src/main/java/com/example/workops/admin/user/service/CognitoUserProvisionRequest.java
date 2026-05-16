package com.example.workops.admin.user.service;

/**
 * Cognitoユーザー作成に渡す最小入力。
 */
public record CognitoUserProvisionRequest(
        String username,
        String email) {
}
