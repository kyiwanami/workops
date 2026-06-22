package com.example.workops.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/** Spring Securityのlocal logout後に、Cognito Hosted UIのlogout endpointへ遷移させる。 */
@Component
public class CognitoLogoutSuccessHandler implements LogoutSuccessHandler {

  private static final String ACTOR_TYPE_PLATFORM = "PLATFORM";
  private static final String ACTOR_TYPE_TENANT = "TENANT";
  private static final String LOCAL_LOGIN_PATH = "/login";

  private final String hostedUiDomainBaseUrl;
  private final String logoutUri;
  private final String platformClientId;
  private final String tenantClientId;

  public CognitoLogoutSuccessHandler(
      @Value("${WORKOPS_COGNITO_HOSTED_UI_DOMAIN_BASE_URL:}") String hostedUiDomainBaseUrl,
      @Value("${WORKOPS_COGNITO_LOGOUT_URI:/login}") String logoutUri,
      @Value("${WORKOPS_COGNITO_PLATFORM_CLIENT_ID:}") String platformClientId,
      @Value("${WORKOPS_COGNITO_TENANT_CLIENT_ID:}") String tenantClientId) {
    this.hostedUiDomainBaseUrl = hostedUiDomainBaseUrl;
    this.logoutUri = logoutUri;
    this.platformClientId = platformClientId;
    this.tenantClientId = tenantClientId;
  }

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    String clientId = clientId(authentication);
    URI hostedUiBaseUri = hostedUiBaseUri();
    if (hostedUiBaseUri == null || logoutUri.isEmpty() || clientId.isEmpty()) {
      response.sendRedirect(LOCAL_LOGIN_PATH);
      return;
    }

    response.sendRedirect(
        hostedUiBaseUri
            + "/logout?client_id="
            + encode(clientId)
            + "&logout_uri="
            + encode(logoutUri));
  }

  private URI hostedUiBaseUri() {
    if (hostedUiDomainBaseUrl.isEmpty() || containsLineBreak(hostedUiDomainBaseUrl)) {
      return null;
    }
    try {
      URI uri = new URI(hostedUiDomainBaseUrl);
      if (!"https".equals(uri.getScheme()) || uri.getHost() == null) {
        return null;
      }
      return uri;
    } catch (URISyntaxException exception) {
      return null;
    }
  }

  private boolean containsLineBreak(String value) {
    return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
  }

  private String clientId(Authentication authentication) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof LoginUserContext loginUserContext)) {
      return "";
    }
    if (ACTOR_TYPE_PLATFORM.equals(loginUserContext.actorType())) {
      return platformClientId;
    }
    if (ACTOR_TYPE_TENANT.equals(loginUserContext.actorType())) {
      return tenantClientId;
    }

    return "";
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
