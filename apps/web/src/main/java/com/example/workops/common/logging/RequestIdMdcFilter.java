package com.example.workops.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** HTTP request単位のログ相関IDをMDCとresponse headerへ設定するFilter。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdMdcFilter extends OncePerRequestFilter {

  static final String MDC_REQUEST_ID = "requestId";
  static final String RESPONSE_HEADER_REQUEST_ID = "X-Request-Id";
  public static final String REQUEST_ATTRIBUTE_REQUEST_ID = "workops.requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    request.setAttribute(REQUEST_ATTRIBUTE_REQUEST_ID, requestId);
    response.setHeader(RESPONSE_HEADER_REQUEST_ID, requestId);

    MDC.put(MDC_REQUEST_ID, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_REQUEST_ID);
    }
  }
}
