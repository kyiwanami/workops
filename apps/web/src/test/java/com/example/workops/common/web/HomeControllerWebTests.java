package com.example.workops.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workops.common.logging.ApplicationExceptionLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(
    controllers = HomeController.class,
    excludeAutoConfiguration = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerWebTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CurrentUserProvider currentUserProvider;

  @MockitoBean private ApplicationExceptionLogger applicationExceptionLogger;

  @Test
  void platformAdminHomeRendersOnlyPlatformLinks() throws Exception {
    signIn("PLATFORM_ADMIN");

    mockMvc
        .perform(homeRequest())
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/admin/companies\"")))
        .andExpect(content().string(containsString("href=\"/admin/companies/1/departments\"")))
        .andExpect(content().string(containsString("href=\"/admin/users\"")))
        .andExpect(content().string(not(containsString("href=\"/requests\""))))
        .andExpect(content().string(not(containsString("href=\"/assets\""))))
        .andExpect(content().string(not(containsString("href=\"/masters/request-types\""))))
        .andExpect(content().string(not(containsString("href=\"/masters/asset-categories\""))))
        .andExpect(content().string(not(containsString("href=\"/departments\""))))
        .andExpect(content().string(not(containsString("href=\"/users\""))))
        .andExpect(content().string(not(containsString("href=\"/auth/claims\""))))
        .andExpect(content().string(not(containsString("href=\"/auth/authorization/manager\""))));
  }

  @Test
  void tenantManagerHomeRendersTenantBusinessAndTenantManagementLinks() throws Exception {
    signIn("TENANT_MANAGER");

    mockMvc
        .perform(homeRequest())
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/requests\"")))
        .andExpect(content().string(containsString("href=\"/assets\"")))
        .andExpect(content().string(containsString("href=\"/masters/request-types\"")))
        .andExpect(content().string(containsString("href=\"/masters/asset-categories\"")))
        .andExpect(content().string(containsString("href=\"/departments\"")))
        .andExpect(content().string(containsString("href=\"/users\"")))
        .andExpect(content().string(not(containsString("href=\"/admin/companies\""))))
        .andExpect(content().string(not(containsString("href=\"/admin/companies/1/departments\""))))
        .andExpect(content().string(not(containsString("href=\"/admin/users\""))));
  }

  @Test
  void tenantViewerHomeRendersOnlyTenantBusinessLinks() throws Exception {
    signIn("TENANT_VIEWER");

    mockMvc
        .perform(homeRequest())
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/requests\"")))
        .andExpect(content().string(containsString("href=\"/assets\"")))
        .andExpect(content().string(not(containsString("href=\"/masters/request-types\""))))
        .andExpect(content().string(not(containsString("href=\"/masters/asset-categories\""))))
        .andExpect(content().string(not(containsString("href=\"/departments\""))))
        .andExpect(content().string(not(containsString("href=\"/users\""))))
        .andExpect(content().string(not(containsString("href=\"/admin/companies\""))))
        .andExpect(content().string(not(containsString("href=\"/admin/users\""))));
  }

  @Test
  void tenantEditorHomeRendersOnlyTenantBusinessLinks() throws Exception {
    signIn("TENANT_EDITOR");

    mockMvc
        .perform(homeRequest())
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/requests\"")))
        .andExpect(content().string(containsString("href=\"/assets\"")))
        .andExpect(content().string(not(containsString("href=\"/masters/request-types\""))))
        .andExpect(content().string(not(containsString("href=\"/masters/asset-categories\""))))
        .andExpect(content().string(not(containsString("href=\"/departments\""))))
        .andExpect(content().string(not(containsString("href=\"/users\""))))
        .andExpect(content().string(not(containsString("href=\"/admin/companies\""))))
        .andExpect(content().string(not(containsString("href=\"/admin/users\""))));
  }

  private void signIn(String permissionCode) {
    LoginUserContext loginUserContext = user(permissionCode);
    when(currentUserProvider.requireCurrentUser()).thenReturn(loginUserContext);
    when(currentUserProvider.currentUser()).thenReturn(Optional.of(loginUserContext));
  }

  private MockHttpServletRequestBuilder homeRequest() {
    return get("/")
        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
  }

  private LoginUserContext user(String permissionCode) {
    Long companyId = 1L;
    String actorType = "TENANT";
    if (permissionCode.equals("PLATFORM_ADMIN")) {
      companyId = null;
      actorType = "PLATFORM";
    }
    return new LoginUserContext(
        10L,
        "user",
        "user@example.com",
        actorType,
        companyId,
        List.of(new PermissionSetContext(permissionCode, permissionCode)));
  }
}
