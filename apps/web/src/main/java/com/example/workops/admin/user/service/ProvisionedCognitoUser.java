package com.example.workops.admin.user.service;

/**
 * Cognitoユーザー作成後にWorkOps DBへ登録する識別子。
 *
 * @param cognitoSub {@code users.cognito_sub} に保存するCognito {@code sub}
 */
public record ProvisionedCognitoUser(String cognitoSub) {}
