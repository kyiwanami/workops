package com.example.workops.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spring SecurityのCognito subからDB由来の現在ユーザーを取得するProvider。
 */
@Component
@Profile("!local")
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    private final DbLoginUserContextFactory dbLoginUserContextFactory;

    public SecurityCurrentUserProvider(DbLoginUserContextFactory dbLoginUserContextFactory) {
        this.dbLoginUserContextFactory = dbLoginUserContextFactory;
    }

    @Override
    public Optional<LoginUserContext> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return Optional.empty();
        }

        return dbLoginUserContextFactory.fromCognitoSub(oidcUser.getSubject());
    }
}
