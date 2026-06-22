package com.example.workops.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdMdcFilterTests {

  private final RequestIdMdcFilter requestIdMdcFilter = new RequestIdMdcFilter();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void addsRequestIdHeaderAndMdcDuringRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] requestIdDuringFilter = new String[1];

    requestIdMdcFilter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) ->
            requestIdDuringFilter[0] = MDC.get(RequestIdMdcFilter.MDC_REQUEST_ID));

    String responseHeader = response.getHeader(RequestIdMdcFilter.RESPONSE_HEADER_REQUEST_ID);
    assertThat(responseHeader).isNotBlank();
    assertThat(UUID.fromString(responseHeader)).isNotNull();
    assertThat(requestIdDuringFilter[0]).isEqualTo(responseHeader);
    assertThat(request.getAttribute(RequestIdMdcFilter.REQUEST_ATTRIBUTE_REQUEST_ID))
        .isEqualTo(responseHeader);
  }

  @Test
  void removesMdcRequestIdAfterRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    requestIdMdcFilter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    assertThat(MDC.get(RequestIdMdcFilter.MDC_REQUEST_ID)).isNull();
  }

  @Test
  void removesMdcRequestIdWhenRequestFails() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(
            () ->
                requestIdMdcFilter.doFilter(
                    request,
                    response,
                    (servletRequest, servletResponse) -> {
                      throw new ServletException("request failed");
                    }))
        .isInstanceOf(ServletException.class);

    assertThat(response.getHeader(RequestIdMdcFilter.RESPONSE_HEADER_REQUEST_ID)).isNotBlank();
    assertThat(MDC.get(RequestIdMdcFilter.MDC_REQUEST_ID)).isNull();
  }
}
