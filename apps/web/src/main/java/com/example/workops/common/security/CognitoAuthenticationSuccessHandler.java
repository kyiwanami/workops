package com.example.workops.common.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.example.workops.common.logging.SecurityEventLogger;
import com.example.workops.common.logging.SecurityEventLogRecord;

/**
 * Cognitoログイン後にDB由来のWorkOpsユーザーをSpring Securityの現在ユーザーとして保存する。
 */
@Component
public class CognitoAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String EVENT_TYPE_AUTHENTICATION_REJECTED = "AUTHENTICATION_REJECTED";
    private static final String EVENT_TYPE_AUTHENTICATION_SUCCEEDED = "AUTHENTICATION_SUCCEEDED";
    private static final String REASON_CODE_OIDC_PRINCIPAL_MISSING = "OIDC_PRINCIPAL_MISSING";
    private static final String REASON_CODE_USER_NOT_LINKED = "USER_NOT_LINKED";
    private static final String REASON_CODE_ACTOR_TYPE_MISMATCH = "ACTOR_TYPE_MISMATCH";

    private final DbLoginUserContextFactory dbLoginUserContextFactory;
    private final WorkOpsAuthenticationFactory workOpsAuthenticationFactory;
    private final SecurityEventLogger securityEventLogger;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public CognitoAuthenticationSuccessHandler(
            DbLoginUserContextFactory dbLoginUserContextFactory,
            WorkOpsAuthenticationFactory workOpsAuthenticationFactory,
            SecurityEventLogger securityEventLogger) {
        this.dbLoginUserContextFactory = dbLoginUserContextFactory;
        this.workOpsAuthenticationFactory = workOpsAuthenticationFactory;
        this.securityEventLogger = securityEventLogger;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            securityEventLogger.logRejected(new SecurityEventLogRecord(
                    null,
                    EVENT_TYPE_AUTHENTICATION_REJECTED,
                    REASON_CODE_OIDC_PRINCIPAL_MISSING,
                    null));
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        LoginUserContext loginUserContext = dbLoginUserContextFactory.fromCognitoSub(oidcUser.getSubject()).orElse(null);
        if (loginUserContext == null) {
            securityEventLogger.logRejected(new SecurityEventLogRecord(
                    null,
                    EVENT_TYPE_AUTHENTICATION_REJECTED,
                    REASON_CODE_USER_NOT_LINKED,
                    null));
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        LoginRouteActorType expectedActorType = extractExpectedActorType(authentication);
        if (!expectedActorType.matches(loginUserContext.actorType())) {
            securityEventLogger.logRejected(new SecurityEventLogRecord(
                    loginUserContext,
                    EVENT_TYPE_AUTHENTICATION_REJECTED,
                    REASON_CODE_ACTOR_TYPE_MISMATCH,
                    null));
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        securityEventLogger.logSuccess(new SecurityEventLogRecord(
                loginUserContext,
                EVENT_TYPE_AUTHENTICATION_SUCCEEDED,
                null,
                null));
        Authentication workOpsAuthentication = workOpsAuthenticationFactory.create(loginUserContext);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(workOpsAuthentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
        response.sendRedirect("/");
    }

    private LoginRouteActorType extractExpectedActorType(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            return LoginRouteActorType.fromRegistrationId(oauth2Authentication.getAuthorizedClientRegistrationId());
        }

        throw new IllegalStateException("Cognito authentication must be OAuth2AuthenticationToken");
    }
}
