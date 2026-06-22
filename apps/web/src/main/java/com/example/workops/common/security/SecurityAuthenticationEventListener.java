package com.example.workops.common.security;

import com.example.workops.common.logging.SecurityEventLogRecord;
import com.example.workops.common.logging.SecurityEventLogger;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

/** Spring Securityの認証失敗イベントを調査用security eventとして出力する。 */
@Component
public class SecurityAuthenticationEventListener {

  private static final String EVENT_TYPE_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

  private final SecurityEventLogger securityEventLogger;

  public SecurityAuthenticationEventListener(SecurityEventLogger securityEventLogger) {
    this.securityEventLogger = securityEventLogger;
  }

  @EventListener
  public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    securityEventLogger.logRejected(
        new SecurityEventLogRecord(
            null,
            EVENT_TYPE_AUTHENTICATION_FAILED,
            EVENT_TYPE_AUTHENTICATION_FAILED,
            event.getException().getClass().getSimpleName()));
  }
}
