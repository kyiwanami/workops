package com.example.workops.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

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
class ApplicationExceptionLoggerTests {

  private final ApplicationExceptionLogger applicationExceptionLogger =
      new ApplicationExceptionLogger();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void logOutputsKeyValueMessageAndStackTrace(CapturedOutput output) {
    MDC.put("requestId", "request-001");

    applicationExceptionLogger.log(
        new ApplicationExceptionLogRecord(
            "APPLICATION_EXCEPTION", 500, "/sample/path", "IllegalStateException"),
        new IllegalStateException("body-value"));

    String logs = output.toString();
    assertThat(logs).contains("ERROR");
    assertThat(logs).contains("com.example.workops.application");
    assertThat(applicationExceptionLine(logs))
        .contains(
            "requestId=request-001 eventType=APPLICATION_EXCEPTION status=500 "
                + "path=/sample/path exceptionType=IllegalStateException");
    assertThat(logs).contains("java.lang.IllegalStateException");
  }

  @Test
  void emptyOptionalValuesAreLoggedAsHyphen(CapturedOutput output) {
    applicationExceptionLogger.log(
        new ApplicationExceptionLogRecord("", 500, "", ""), new IllegalStateException("message"));

    assertThat(applicationExceptionLine(output.toString()))
        .contains("requestId=- eventType=- status=500 path=- exceptionType=-");
  }

  @Test
  void keyValueLineDoesNotContainExceptionMessage(CapturedOutput output) {
    applicationExceptionLogger.log(
        new ApplicationExceptionLogRecord(
            "APPLICATION_EXCEPTION", 500, "/sample/path", "IllegalStateException"),
        new IllegalStateException("token=secret-value"));

    assertThat(applicationExceptionLine(output.toString()))
        .doesNotContain("token=secret-value", "secret-value");
  }

  @Test
  void applicationExceptionLogRecordDoesNotHaveSensitiveFields() {
    List<String> componentNames =
        Arrays.stream(ApplicationExceptionLogRecord.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

    assertThat(componentNames)
        .doesNotContain(
            "message",
            "queryString",
            "parameter",
            "body",
            "token",
            "password",
            "secret",
            "connectionString");
  }

  private String applicationExceptionLine(String logs) {
    return logs.lines()
        .filter(
            line ->
                line.contains("eventType=APPLICATION_EXCEPTION") || line.contains("eventType=-"))
        .findFirst()
        .orElse("");
  }
}
