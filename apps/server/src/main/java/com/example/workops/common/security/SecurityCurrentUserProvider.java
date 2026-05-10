package com.example.workops.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Spring Securityの認証情報からCognito subだけを取得するProvider。
 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<LoginUserContext> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return Optional.empty();
        }

        return Optional.of(new LoginUserContext(oidcUser.getSubject()));
    }
}
