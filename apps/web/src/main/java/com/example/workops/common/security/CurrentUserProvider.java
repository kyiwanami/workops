package com.example.workops.common.security;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ControllerやServiceが認証基盤へ直接依存せず、現在ユーザーを取得するための境界。
 *
 * <p>現在ユーザー情報はSpring Securityの {@code Authentication.principal} に格納された {@link LoginUserContext}
 * を正本として扱う。
 */
@Component
public class CurrentUserProvider {

  /**
   * SecurityContextから現在ユーザーを取得する。
   *
   * @return 未認証、またはprincipalが {@link LoginUserContext} でない場合は空
   */
  public Optional<LoginUserContext> currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof LoginUserContext loginUserContext)) {
      return Optional.empty();
    }

    return Optional.of(loginUserContext);
  }

  /**
   * SecurityContextから現在ユーザーを必須取得する。
   *
   * @return 現在ユーザーコンテキスト
   * @throws AccessDeniedException 現在ユーザーを取得できない場合
   */
  public LoginUserContext requireCurrentUser() {
    return currentUser().orElseThrow(() -> new AccessDeniedException("現在ユーザーを取得できません。"));
  }
}
