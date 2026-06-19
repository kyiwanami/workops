package com.example.workops.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class FaviconResourceTests {

    @Test
    void faviconIsServedAsStaticResource() throws Exception {
        ClassPathResource favicon = new ClassPathResource("static/favicon.ico");

        assertThat(favicon.exists()).isTrue();
        assertThat(favicon.contentLength()).isGreaterThan(0);
    }
}
