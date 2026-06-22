package com.example.workops.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class SecurityEventLoggerTests {

  private final SecurityEventLogger securityEventLogger = new SecurityEventLogger();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void logSuccessOutputsKeyValueMessage(CapturedOutput output) {
    MDC.put("requestId", "request-001");

    securityEventLogger.logSuccess(
        new SecurityEventLogRecord(
            loginUserContext(
                List.of(permission("TENANT_EDITOR", "編集者"), permission("TENANT_MANAGER", "管理者"))),
            "AUTHENTICATION_SUCCEEDED",
            null,
            null));

    String logs = output.toString();
    assertThat(logs).contains("INFO");
    assertThat(logs).contains("com.example.workops.security");
    assertThat(logs)
        .contains(
            "requestId=request-001 userId=10 companyId=1 actorType=TENANT "
                + "authorities=TENANT_EDITOR,TENANT_MANAGER "
                + "eventType=AUTHENTICATION_SUCCEEDED result=SUCCESS "
                + "reasonCode=- exceptionType=-");
  }

  @Test
  void logRejectedOutputsWarnAndReasonCode(CapturedOutput output) {
    securityEventLogger.logRejected(
        new SecurityEventLogRecord(
            loginUserContext(List.of(permission("TENANT_MANAGER", "管理者"))),
            "AUTHORIZATION_DENIED",
            "AUTHORIZATION_DENIED",
            "AccessDeniedException"));

    String logs = output.toString();
    assertThat(logs).contains("WARN");
    assertThat(logs)
        .contains(
            "requestId=- userId=10 companyId=1 actorType=TENANT "
                + "authorities=TENANT_MANAGER eventType=AUTHORIZATION_DENIED "
                + "result=REJECTED reasonCode=AUTHORIZATION_DENIED "
                + "exceptionType=AccessDeniedException");
  }

  @Test
  void emptyOptionalValuesAreLoggedAsHyphen(CapturedOutput output) {
    securityEventLogger.logRejected(new SecurityEventLogRecord(null, "", "", ""));

    String logs = output.toString();
    assertThat(logs)
        .contains(
            "requestId=- userId=- companyId=- actorType=- authorities=- "
                + "eventType=- result=REJECTED reasonCode=- exceptionType=-");
  }

  @Test
  void securityEventLogRecordDoesNotHaveSensitiveFields() {
    List<String> componentNames =
        Arrays.stream(SecurityEventLogRecord.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

    assertThat(componentNames)
        .doesNotContain("sub", "subHash", "email", "username", "token", "secret", "password");
  }

  private LoginUserContext loginUserContext(List<PermissionSetContext> permissionSets) {
    return new LoginUserContext(
        10L, "test-user", "test-user@example.local", "TENANT", 1L, permissionSets);
  }

  private PermissionSetContext permission(String code, String name) {
    return new PermissionSetContext(code, name);
  }
}
