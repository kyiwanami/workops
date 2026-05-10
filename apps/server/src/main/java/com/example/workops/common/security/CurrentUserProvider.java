package com.example.workops.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ControllerやServiceが認証基盤へ直接依存せず、現在ユーザーを取得するための境界。
 */
@Component
public class CurrentUserProvider {

    public Optional<LoginUserContext> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUserContext loginUserContext)) {
            return Optional.empty();
        }

        return Optional.of(loginUserContext);
    }
}
