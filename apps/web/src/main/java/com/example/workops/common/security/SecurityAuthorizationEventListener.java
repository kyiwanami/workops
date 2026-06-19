package com.example.workops.common.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.workops.common.logging.SecurityEventLogger;
import com.example.workops.common.logging.SecurityEventLogRecord;

/**
 * Spring Securityの認可拒否イベントを調査用security eventとして出力する。
 */
@Component
public class SecurityAuthorizationEventListener {

    private static final String EVENT_TYPE_AUTHORIZATION_DENIED = "AUTHORIZATION_DENIED";

    private final SecurityEventLogger securityEventLogger;

    public SecurityAuthorizationEventListener(SecurityEventLogger securityEventLogger) {
        this.securityEventLogger = securityEventLogger;
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        securityEventLogger.logRejected(new SecurityEventLogRecord(
                loginUserContext(event.getAuthentication().get()),
                EVENT_TYPE_AUTHORIZATION_DENIED,
                EVENT_TYPE_AUTHORIZATION_DENIED,
                AccessDeniedException.class.getSimpleName()));
    }

    private LoginUserContext loginUserContext(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserContext loginUserContext) {
            return loginUserContext;
        }
        return null;
    }
}
