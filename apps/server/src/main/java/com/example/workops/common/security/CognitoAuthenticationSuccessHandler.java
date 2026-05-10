package com.example.workops.common.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
}
