package com.example.workops.common.logging;

import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

/**
 * 主要な更新系業務操作をkey=value形式で出力するロガー。
 */
@Component
public class OperationLogger {

    private static final Logger OPERATION_LOGGER = LoggerFactory.getLogger("com.example.workops.operation");
    private static final String EMPTY_VALUE = "-";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String SUCCESS_RESULT = "SUCCESS";
    private static final String REJECTED_RESULT = "REJECTED";

    public void logSuccess(OperationLogRecord record) {
        OPERATION_LOGGER.info(format(record, SUCCESS_RESULT));
    }

    public void logRejected(OperationLogRecord record) {
        OPERATION_LOGGER.warn(format(record, REJECTED_RESULT));
    }

    private String format(OperationLogRecord record, String result) {
        LoginUserContext loginUserContext = record.loginUserContext();
        return "requestId=" + valueOrEmpty(MDC.get(MDC_REQUEST_ID))
                + " userId=" + valueOrEmpty(loginUserContext.userId())
                + " companyId=" + valueOrEmpty(loginUserContext.companyId())
                + " actorType=" + valueOrEmpty(loginUserContext.actorType())
                + " authorities=" + authorities(loginUserContext)
                + " operation=" + valueOrEmpty(record.operation())
                + " targetType=" + valueOrEmpty(record.targetType())
                + " targetId=" + valueOrEmpty(record.targetId())
                + " result=" + result
                + " reasonCode=" + valueOrEmpty(record.reasonCode())
                + " reasonCommentPresent=" + record.reasonCommentPresent()
                + " exceptionType=" + valueOrEmpty(record.exceptionType());
    }

    private String authorities(LoginUserContext loginUserContext) {
        if (loginUserContext.permissionSets().isEmpty()) {
            return EMPTY_VALUE;
        }

        StringJoiner joiner = new StringJoiner(",");
        for (PermissionSetContext permissionSet : loginUserContext.permissionSets()) {
            joiner.add(valueOrEmpty(permissionSet.code()));
        }
        return joiner.toString();
    }

    private String valueOrEmpty(Long value) {
        if (value == null) {
            return EMPTY_VALUE;
        }
        return value.toString();
    }

    private String valueOrEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY_VALUE;
        }
        return value;
    }
}
