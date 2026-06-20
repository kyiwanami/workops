package com.example.workops.common.web;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateAuthNavigationTests {

    @Test
    void loginTemplateShowsTenantEntrypointOnly() throws Exception {
        String template = template("templates/login.html");

        assertThat(template).contains("@{/login/tenant}");
        assertThat(template).doesNotContain("@{/login/platform}");
    }

    @Test
    void layoutTemplatePostsLogoutWithCsrf() throws Exception {
        String template = template("templates/layout.html");

        assertThat(template).contains("th:if=\"${currentUser != null}\"");
        assertThat(template).contains("th:action=\"@{/logout}\"");
        assertThat(template).contains("method=\"post\"");
        assertThat(template).contains("th:name=\"${_csrf.parameterName}\"");
        assertThat(template).contains("th:value=\"${_csrf.token}\"");
        assertThat(template).doesNotContain("href=\"/logout\"");
    }

    private String template(String path) throws Exception {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
