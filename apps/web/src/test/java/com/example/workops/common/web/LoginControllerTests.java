package com.example.workops.common.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginControllerTests {

    @Test
    void loginShowsTenantFacingLoginPage() {
        LoginController controller = new LoginController();

        assertThat(controller.login()).isEqualTo("login");
    }

    @Test
    void platformLoginStartsPlatformOAuth2Flow() {
        LoginController controller = new LoginController();

        assertThat(controller.platformLogin()).isEqualTo("redirect:/oauth2/authorization/platform");
    }

    @Test
    void tenantLoginStartsTenantOAuth2Flow() {
        LoginController controller = new LoginController();

        assertThat(controller.tenantLogin()).isEqualTo("redirect:/oauth2/authorization/tenant");
    }
}
