package com.example.workops.common.logging;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class OperationLoggerTests {

    private final OperationLogger operationLogger = new OperationLogger();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void logSuccessOutputsKeyValueMessage(CapturedOutput output) {
        MDC.put("requestId", "request-001");

        operationLogger.logSuccess(new OperationLogRecord(
                loginUserContext(List.of(
                        permission("TENANT_EDITOR", "編集者"),
                        permission("TENANT_MANAGER", "管理者"))),
                "REQUEST_APPROVE",
                "REQUEST",
                123L,
                null,
                false,
                null));

        String logs = output.toString();
        assertThat(logs).contains("INFO");
        assertThat(logs).contains("com.example.workops.operation");
        assertThat(logs).contains(
                "requestId=request-001 userId=10 companyId=1 actorType=TENANT "
                        + "authorities=TENANT_EDITOR,TENANT_MANAGER operation=REQUEST_APPROVE "
                        + "targetType=REQUEST targetId=123 result=SUCCESS reasonCode=- "
                        + "reasonCommentPresent=false exceptionType=-");
    }

    @Test
    void logRejectedOutputsWarnAndReasonCode(CapturedOutput output) {
        operationLogger.logRejected(new OperationLogRecord(
                loginUserContext(List.of(permission("TENANT_MANAGER", "管理者"))),
                "REQUEST_SUBMIT",
                "REQUEST",
                456L,
                "STATUS_MISMATCH",
                true,
                "AccessDeniedException"));

        String logs = output.toString();
        assertThat(logs).contains("WARN");
        assertThat(logs).contains(
                "requestId=- userId=10 companyId=1 actorType=TENANT "
                        + "authorities=TENANT_MANAGER operation=REQUEST_SUBMIT "
                        + "targetType=REQUEST targetId=456 result=REJECTED reasonCode=STATUS_MISMATCH "
                        + "reasonCommentPresent=true exceptionType=AccessDeniedException");
    }

    @Test
    void emptyOptionalValuesAreLoggedAsHyphen(CapturedOutput output) {
        operationLogger.logSuccess(new OperationLogRecord(
                loginUserContext(List.of()),
                "",
                "",
                null,
                "",
                false,
                ""));

        String logs = output.toString();
        assertThat(logs).contains(
                "requestId=- userId=10 companyId=1 actorType=TENANT authorities=- "
                        + "operation=- targetType=- targetId=- result=SUCCESS reasonCode=- "
                        + "reasonCommentPresent=false exceptionType=-");
    }

    @Test
    void operationLogRecordDoesNotHaveBusinessTextFields() {
        List<String> componentNames = Arrays.stream(OperationLogRecord.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(componentNames)
                .doesNotContain(
                        "title",
                        "content",
                        "reviewComment",
                        "assetName",
                        "email",
                        "name",
                        "beforeJson",
                        "afterJson");
    }

    private LoginUserContext loginUserContext(List<PermissionSetContext> permissionSets) {
        return new LoginUserContext(
                10L,
                "test-user",
                "test-user@example.local",
                "TENANT",
                1L,
                permissionSets);
    }

    private PermissionSetContext permission(String code, String name) {
        return new PermissionSetContext(code, name);
    }
}
