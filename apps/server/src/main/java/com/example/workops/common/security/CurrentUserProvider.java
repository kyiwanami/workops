package com.example.workops.common.security;

import java.util.Optional;

/**
 * ControllerやServiceが認証基盤へ直接依存せず、現在ユーザーを取得するための境界。
 */
public interface CurrentUserProvider {

    Optional<LoginUserContext> currentUser();
}
