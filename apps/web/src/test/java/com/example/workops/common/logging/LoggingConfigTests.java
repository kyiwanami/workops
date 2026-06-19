package com.example.workops.common.logging;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingConfigTests {

    @Test
    void applicationAddsRequestIdToLogLevelPatternWithoutConsoleOverride() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(
                "application",
                new ClassPathResource("application.yml"));

        assertThat(propertySources)
                .anySatisfy(propertySource -> assertThat(propertySource.getProperty("logging.pattern.level"))
                        .isEqualTo("requestId=%X{requestId:--} %5p"));
        assertThat(propertySources)
                .noneSatisfy(propertySource -> assertThat(propertySource.containsProperty("logging.pattern.console"))
                        .isTrue());
    }
}
