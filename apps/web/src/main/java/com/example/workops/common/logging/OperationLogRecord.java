package com.example.workops.common.logging;

import com.example.workops.common.security.LoginUserContext;

/**
 * 業務操作ログに出力するID中心の操作情報。
 */
public record OperationLogRecord(
        LoginUserContext loginUserContext,
        String operation,
        String targetType,
        Long targetId,
        String reasonCode,
        boolean reasonCommentPresent,
        String exceptionType) {
}
