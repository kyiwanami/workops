package com.example.workops.common.web;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.logging.ApplicationExceptionLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class ApplicationExceptionHandlerTests {

    private final ApplicationExceptionHandler applicationExceptionHandler = new ApplicationExceptionHandler(
            new ApplicationExceptionLogger());

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void runtimeExceptionOutputsApplicationExceptionAndReturnsErrorView(CapturedOutput output) {
        MDC.put("requestId", "request-001");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sample/path");
        request.setQueryString("token=secret-value");
        request.addParameter("password", "form-password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ExtendedModelMap();

        String view = applicationExceptionHandler.handleException(
                new IllegalStateException("secret-value"),
                request,
                response,
                model);

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(model.asMap()).containsEntry("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(model.asMap()).containsEntry("requestId", "request-001");
        assertThat(applicationExceptionLine(output.toString())).contains(
                "requestId=request-001 eventType=APPLICATION_EXCEPTION status=500 "
                        + "path=/sample/path exceptionType=IllegalStateException");
        assertThat(applicationExceptionLine(output.toString()))
                .doesNotContain("token=secret-value", "form-password", "secret-value", "password");
    }

    @Test
    void responseStatusExceptionUsesDeclaredStatus(CapturedOutput output) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ExtendedModelMap();

        String view = applicationExceptionHandler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "not found value"),
                request,
                response,
                model);

        assertThat(view).isEqualTo("error");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(model.asMap()).containsEntry("status", HttpStatus.NOT_FOUND.value());
        assertThat(applicationExceptionLine(output.toString())).contains(
                "requestId=- eventType=APPLICATION_EXCEPTION status=404 "
                        + "path=/missing exceptionType=ResponseStatusException");
        assertThat(applicationExceptionLine(output.toString())).doesNotContain("not found value");
    }

    @Test
    void securityExceptionsAreRethrown() {
        assertThatThrownBy(() -> applicationExceptionHandler.handleSecurityException(
                new AccessDeniedException("denied")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> applicationExceptionHandler.handleSecurityException(
                new BadCredentialsException("bad credentials")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void errorTemplateDisplaysOnlyStatusAndRequestId() throws Exception {
        ClassPathResource resource = new ClassPathResource("templates/error.html");

        String template = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(template).contains("${status}", "${requestId}");
        assertThat(template).doesNotContain(
                "${exception}",
                "${message}",
                "${path}",
                "${trace}",
                "${error}");
    }

    private String applicationExceptionLine(String logs) {
        return logs.lines()
                .filter(line -> line.contains("eventType=APPLICATION_EXCEPTION"))
                .findFirst()
                .orElse("");
    }
}
