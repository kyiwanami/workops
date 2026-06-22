package com.example.workops.common.web;

import com.example.workops.common.logging.RequestIdMdcFilter;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/** Spring Bootの/error経路でも画面へrequestIdを渡すErrorAttributes。 */
@Component
public class WorkOpsErrorAttributes extends DefaultErrorAttributes {

  private static final String REQUEST_ID_ATTRIBUTE = "requestId";
  private static final String EMPTY_VALUE = "-";

  @Override
  public Map<String, Object> getErrorAttributes(
      WebRequest webRequest, ErrorAttributeOptions options) {
    Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);
    attributes.put(REQUEST_ID_ATTRIBUTE, requestId(webRequest));
    return attributes;
  }

  private String requestId(WebRequest webRequest) {
    Object requestId =
        webRequest.getAttribute(
            RequestIdMdcFilter.REQUEST_ATTRIBUTE_REQUEST_ID, RequestAttributes.SCOPE_REQUEST);
    if (requestId instanceof String value && !value.isEmpty()) {
      return value;
    }

    String mdcRequestId = MDC.get(REQUEST_ID_ATTRIBUTE);
    if (mdcRequestId == null || mdcRequestId.isEmpty()) {
      return EMPTY_VALUE;
    }
    return mdcRequestId;
  }
}
