package com.example.workops.admin.user.service;

/**
 * Cognitoユーザー作成後にWorkOps DBへ登録する識別子。
 */
public record ProvisionedCognitoUser(String cognitoSub) {
}
