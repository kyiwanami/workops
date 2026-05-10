package com.example.workops.common.security;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Spring Securityの認証情報からWorkOps内部のログインユーザーコンテキストを作るProvider。
 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<LoginUserContext> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return Optional.empty();
        }

        LoginUserContext context = new LoginUserContext(
                stringValue(oidcUser.getSubject()),
                stringClaim(oidcUser, "cognito:username"),
                stringValue(oidcUser.getEmail()));
        return Optional.of(context);
    }

    private String stringClaim(OidcUser oidcUser, String claimName) {
        Map<String, Object> claims = oidcUser.getClaims();
        Object value = claims.get(claimName);
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String stringValue(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
