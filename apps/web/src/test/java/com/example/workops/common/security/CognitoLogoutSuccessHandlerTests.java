package com.example.workops.common.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoLogoutSuccessHandlerTests {

    private static final String HOSTED_UI_DOMAIN_BASE_URL = "https://workops-dev.auth.ap-northeast-1.amazoncognito.com";
    private static final String LOGOUT_URI = "https://d111111abcdef8.cloudfront.net/login";
    private static final String PLATFORM_CLIENT_ID = "platform-client-id";
    private static final String TENANT_CLIENT_ID = "tenant-client-id";

    @Test
    void platformUserRedirectsToCognitoLogoutWithPlatformClientId() throws Exception {
        CognitoLogoutSuccessHandler handler = handler();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(new MockHttpServletRequest(), response, authentication(platformUser()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo(HOSTED_UI_DOMAIN_BASE_URL
                        + "/logout?client_id=platform-client-id"
                        + "&logout_uri=https%3A%2F%2Fd111111abcdef8.cloudfront.net%2Flogin");
    }

    @Test
    void tenantUserRedirectsToCognitoLogoutWithTenantClientId() throws Exception {
        CognitoLogoutSuccessHandler handler = handler();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(new MockHttpServletRequest(), response, authentication(tenantUser()));

        assertThat(response.getRedirectedUrl())
                .isEqualTo(HOSTED_UI_DOMAIN_BASE_URL
                        + "/logout?client_id=tenant-client-id"
                        + "&logout_uri=https%3A%2F%2Fd111111abcdef8.cloudfront.net%2Flogin");
    }

    @Test
    void unknownPrincipalReturnsLogin() throws Exception {
        CognitoLogoutSuccessHandler handler = handler();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(
                new MockHttpServletRequest(),
                response,
                new UsernamePasswordAuthenticationToken("principal", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
    }

    @Test
    void unknownActorTypeReturnsLogin() throws Exception {
        CognitoLogoutSuccessHandler handler = handler();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(new MockHttpServletRequest(), response, authentication(unknownActorTypeUser()));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
    }

    @Test
    void missingCognitoSettingReturnsLogin() throws Exception {
        CognitoLogoutSuccessHandler handler = new CognitoLogoutSuccessHandler(
                "",
                LOGOUT_URI,
                PLATFORM_CLIENT_ID,
                TENANT_CLIENT_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(new MockHttpServletRequest(), response, authentication(platformUser()));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
    }

    private CognitoLogoutSuccessHandler handler() {
        return new CognitoLogoutSuccessHandler(
                HOSTED_UI_DOMAIN_BASE_URL,
                LOGOUT_URI,
                PLATFORM_CLIENT_ID,
                TENANT_CLIENT_ID);
    }

    private Authentication authentication(LoginUserContext loginUserContext) {
        return new UsernamePasswordAuthenticationToken(loginUserContext, null);
    }

    private LoginUserContext platformUser() {
        return user("PLATFORM");
    }

    private LoginUserContext tenantUser() {
        return user("TENANT");
    }

    private LoginUserContext unknownActorTypeUser() {
        return user("UNKNOWN");
    }

    private LoginUserContext user(String actorType) {
        return new LoginUserContext(
                1L,
                "test-user",
                "test-user@example.local",
                actorType,
                null,
                List.of(new PermissionSetContext("TENANT_MANAGER", "管理者")));
    }
}
