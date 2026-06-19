package com.example.workops.common.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTests {

    @Test
    void applicationEnablesTomcatRelativeRedirects() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(
                "application",
                new ClassPathResource("application.yml"));

        assertThat(propertySources)
                .anySatisfy(propertySource -> assertThat(propertySource.getProperty("server.tomcat.use-relative-redirects"))
                        .isEqualTo(Boolean.TRUE));
    }

    @Test
    void applicationDefinesPlatformAndTenantCognitoRegistrations() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(
                "application",
                new ClassPathResource("application.yml"));

        assertThat(propertySources)
                .anySatisfy(propertySource -> {
                    assertThat(propertySource.getProperty("spring.security.oauth2.client.registration.platform.provider"))
                            .isEqualTo("cognito");
                    assertThat(propertySource.getProperty("spring.security.oauth2.client.registration.platform.client-id"))
                            .isEqualTo("${WORKOPS_COGNITO_PLATFORM_CLIENT_ID:}");
                    assertThat(propertySource.getProperty("spring.security.oauth2.client.registration.platform.redirect-uri"))
                            .isEqualTo("${WORKOPS_COGNITO_PLATFORM_REDIRECT_URI:http://localhost:8080/login/oauth2/code/platform}");
                    assertThat(propertySource.getProperty("spring.security.oauth2.client.registration.tenant.provider"))
                            .isEqualTo("cognito");
                    assertThat(propertySource.getProperty("spring.security.oauth2.client.registration.tenant.client-id"))
                            .isEqualTo("${WORKOPS_COGNITO_TENANT_CLIENT_ID:}");
                    assertThat(propertySource.getProperty("spring.security.oauth2.client.registration.tenant.redirect-uri"))
                            .isEqualTo("${WORKOPS_COGNITO_TENANT_REDIRECT_URI:http://localhost:8080/login/oauth2/code/tenant}");
                });
    }
}
