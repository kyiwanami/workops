package com.example.workops.common.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * local profileでDB由来ユーザーをSpring Securityの現在ユーザーとして設定するFilter。
 */
@Component
@Profile("local")
public class LocalAuthenticationFilter extends OncePerRequestFilter {

    private final DbLoginUserContextFactory dbLoginUserContextFactory;
    private final WorkOpsAuthenticationFactory workOpsAuthenticationFactory;
    private final String localCognitoSub;

    public LocalAuthenticationFilter(
            DbLoginUserContextFactory dbLoginUserContextFactory,
            WorkOpsAuthenticationFactory workOpsAuthenticationFactory,
            @Value("${WORKOPS_LOCAL_COGNITO_SUB:00000000-0000-0000-0000-000000000003}") String localCognitoSub) {
        this.dbLoginUserContextFactory = dbLoginUserContextFactory;
        this.workOpsAuthenticationFactory = workOpsAuthenticationFactory;
        this.localCognitoSub = localCognitoSub;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            dbLoginUserContextFactory.fromCognitoSub(localCognitoSub)
                    .map(workOpsAuthenticationFactory::create)
                    .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        }

        filterChain.doFilter(request, response);
    }
}
