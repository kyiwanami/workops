package com.example.workops.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import com.example.workops.common.logging.RequestIdMdcFilter;

import static org.assertj.core.api.Assertions.assertThat;

class WorkOpsErrorAttributesTests {

    private final WorkOpsErrorAttributes workOpsErrorAttributes = new WorkOpsErrorAttributes();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void addsRequestIdFromRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestIdMdcFilter.REQUEST_ATTRIBUTE_REQUEST_ID, "request-001");

        assertThat(workOpsErrorAttributes.getErrorAttributes(
                new ServletWebRequest(request),
                ErrorAttributeOptions.defaults()))
                .containsEntry("requestId", "request-001");
    }

    @Test
    void addsRequestIdFromMdcWhenRequestAttributeIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        MDC.put("requestId", "request-002");

        assertThat(workOpsErrorAttributes.getErrorAttributes(
                new ServletWebRequest(request),
                ErrorAttributeOptions.defaults()))
                .containsEntry("requestId", "request-002");
    }

    @Test
    void usesEmptyValueWhenRequestIdIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");

        assertThat(workOpsErrorAttributes.getErrorAttributes(
                new ServletWebRequest(request),
                ErrorAttributeOptions.defaults()))
                .containsEntry("requestId", "-");
    }
}
