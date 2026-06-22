package com.example.workops.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.workops.common.logging.SecurityEventLogger;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(OutputCaptureExtension.class)
class CognitoAuthenticationSuccessHandlerTests {

  private static final String COGNITO_SUB = "11111111-2222-3333-4444-555555555555";

  @Test
  void platformLoginStoresWorkOpsAuthentication() throws Exception {
    DbLoginUserContextFactory dbLoginUserContextFactory = mock(DbLoginUserContextFactory.class);
    CognitoAuthenticationSuccessHandler handler =
        new CognitoAuthenticationSuccessHandler(
            dbLoginUserContextFactory,
            new WorkOpsAuthenticationFactory(),
            new SecurityEventLogger());
    when(dbLoginUserContextFactory.fromCognitoSub(COGNITO_SUB))
        .thenReturn(Optional.of(platformUser()));

    MockHttpServletResponse response = new MockHttpServletResponse();
    handler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication("platform"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/");
  }

  @Test
  void platformLoginOutputsAuthenticationSucceeded(CapturedOutput output) throws Exception {
    DbLoginUserContextFactory dbLoginUserContextFactory = mock(DbLoginUserContextFactory.class);
    CognitoAuthenticationSuccessHandler handler =
        new CognitoAuthenticationSuccessHandler(
            dbLoginUserContextFactory,
            new WorkOpsAuthenticationFactory(),
            new SecurityEventLogger());
    when(dbLoginUserContextFactory.fromCognitoSub(COGNITO_SUB))
        .thenReturn(Optional.of(platformUser()));

    MockHttpServletResponse response = new MockHttpServletResponse();
    handler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication("platform"));

    String logs = output.getAll();
    assertThat(logs).contains("com.example.workops.security");
    assertThat(logs)
        .contains(
            "requestId=- userId=1 companyId=- actorType=PLATFORM authorities=PLATFORM_ADMIN "
                + "eventType=AUTHENTICATION_SUCCEEDED result=SUCCESS reasonCode=- exceptionType=-");
    assertThat(logs).doesNotContain(COGNITO_SUB, "platform-admin", "platform-admin@example.local");
  }

  @Test
  void routeActorTypeMismatchReturnsForbiddenAndWarns(CapturedOutput output) throws Exception {
    DbLoginUserContextFactory dbLoginUserContextFactory = mock(DbLoginUserContextFactory.class);
    CognitoAuthenticationSuccessHandler handler =
        new CognitoAuthenticationSuccessHandler(
            dbLoginUserContextFactory,
            new WorkOpsAuthenticationFactory(),
            new SecurityEventLogger());
    when(dbLoginUserContextFactory.fromCognitoSub(COGNITO_SUB))
        .thenReturn(Optional.of(platformUser()));

    MockHttpServletResponse response = new MockHttpServletResponse();
    handler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication("tenant"));

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(output.getAll())
        .contains(
            "eventType=AUTHENTICATION_REJECTED result=REJECTED reasonCode=ACTOR_TYPE_MISMATCH");
  }

  @Test
  void unlinkedCognitoUserReturnsForbiddenAndWarns(CapturedOutput output) throws Exception {
    DbLoginUserContextFactory dbLoginUserContextFactory = mock(DbLoginUserContextFactory.class);
    CognitoAuthenticationSuccessHandler handler =
        new CognitoAuthenticationSuccessHandler(
            dbLoginUserContextFactory,
            new WorkOpsAuthenticationFactory(),
            new SecurityEventLogger());
    when(dbLoginUserContextFactory.fromCognitoSub(COGNITO_SUB)).thenReturn(Optional.empty());

    MockHttpServletResponse response = new MockHttpServletResponse();
    handler.onAuthenticationSuccess(
        new MockHttpServletRequest(), response, authentication("platform"));

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(output.getAll())
        .contains(
            "userId=- companyId=- actorType=- authorities=- eventType=AUTHENTICATION_REJECTED "
                + "result=REJECTED reasonCode=USER_NOT_LINKED exceptionType=-");
    assertThat(output.getAll()).doesNotContain(COGNITO_SUB);
  }

  @Test
  void nonOidcPrincipalReturnsForbiddenAndWarns(CapturedOutput output) throws Exception {
    DbLoginUserContextFactory dbLoginUserContextFactory = mock(DbLoginUserContextFactory.class);
    CognitoAuthenticationSuccessHandler handler =
        new CognitoAuthenticationSuccessHandler(
            dbLoginUserContextFactory,
            new WorkOpsAuthenticationFactory(),
            new SecurityEventLogger());

    MockHttpServletResponse response = new MockHttpServletResponse();
    handler.onAuthenticationSuccess(
        new MockHttpServletRequest(),
        response,
        new UsernamePasswordAuthenticationToken("principal-name", null));

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(output.getAll())
        .contains(
            "eventType=AUTHENTICATION_REJECTED result=REJECTED reasonCode=OIDC_PRINCIPAL_MISSING");
    assertThat(output.getAll()).doesNotContain("principal-name");
  }

  private OAuth2AuthenticationToken authentication(String registrationId) {
    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getSubject()).thenReturn(COGNITO_SUB);

    return new OAuth2AuthenticationToken(oidcUser, List.of(), registrationId);
  }

  private LoginUserContext platformUser() {
    return new LoginUserContext(
        1L,
        "platform-admin",
        "platform-admin@example.local",
        "PLATFORM",
        null,
        List.of(new PermissionSetContext("PLATFORM_ADMIN", "WorkOps管理者")));
  }
}
