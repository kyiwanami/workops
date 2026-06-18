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
}
