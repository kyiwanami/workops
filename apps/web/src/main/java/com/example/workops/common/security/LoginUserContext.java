package com.example.workops.common.security;

import java.util.List;

/**
 * DB由来のアプリ利用者情報をControllerやServiceへ渡す現在ユーザーコンテキスト。
 *
 * <p>Cognitoやlocal認証の結果は、最終的にこの型を {@code Authentication.principal} として保持する。 {@code username}、{@code
 * email}、{@code actorType}、{@code companyId}、権限セットはDB由来の値を使う。
 *
 * @param userId WorkOps DBのユーザーID
 * @param username WorkOps DB由来のユーザー名
 * @param email WorkOps DB由来のメールアドレス
 * @param actorType {@code PLATFORM} または {@code TENANT}
 * @param companyId TENANTユーザーの所属会社ID。PLATFORMユーザーではnull
 * @param permissionSets DB由来の権限セット
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
