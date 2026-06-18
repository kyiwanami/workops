package com.example.workops.common.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Cognitoログイン後にDB由来のWorkOpsユーザーをSpring Securityの現在ユーザーとして保存する。
 */
@Component
public class CognitoAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CognitoAuthenticationSuccessHandler.class);

    private final DbLoginUserContextFactory dbLoginUserContextFactory;
    private final WorkOpsAuthenticationFactory workOpsAuthenticationFactory;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public CognitoAuthenticationSuccessHandler(
            DbLoginUserContextFactory dbLoginUserContextFactory,
            WorkOpsAuthenticationFactory workOpsAuthenticationFactory) {
        this.dbLoginUserContextFactory = dbLoginUserContextFactory;
        this.workOpsAuthenticationFactory = workOpsAuthenticationFactory;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        LoginUserContext loginUserContext = dbLoginUserContextFactory.fromCognitoSub(oidcUser.getSubject()).orElse(null);
        if (loginUserContext == null) {
            LOGGER.warn("Cognito user is not linked to WorkOps user. cognitoSub={}", oidcUser.getSubject());
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        LoginRouteActorType expectedActorType = extractExpectedActorType(authentication);
        if (!expectedActorType.matches(loginUserContext.actorType())) {
            LOGGER.warn(
                    "Cognito login route actor_type mismatch. registrationId={} expectedActorType={} userId={} actualActorType={}",
                    expectedActorType.registrationId(),
                    expectedActorType.actorType(),
                    loginUserContext.userId(),
                    loginUserContext.actorType());
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

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
