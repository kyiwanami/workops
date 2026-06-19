package com.example.workops.common.logging;

import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

/**
 * 認証・認可イベントをkey=value形式で出力するロガー。
 *
 * <p>認証主体の外部IDやメールアドレスは出さず、requestId、イベント種別、理由コード、
 * DB突合後のWorkOps利用者IDを中心にCloudWatch Logsで調査できる値だけを出力する。</p>
 */
@Component
public class SecurityEventLogger {

    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("com.example.workops.security");
    private static final String EMPTY_VALUE = "-";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String SUCCESS_RESULT = "SUCCESS";
    private static final String REJECTED_RESULT = "REJECTED";

    /**
     * 認証・認可の成功結果をINFOログへ出力する。
     *
     * @param record 出力対象の認証・認可イベント情報
     */
    public void logSuccess(SecurityEventLogRecord record) {
        SECURITY_LOGGER.info(format(record, SUCCESS_RESULT));
    }

    /**
     * 認証・認可の拒否または失敗をWARNログへ出力する。
     *
     * @param record 出力対象の認証・認可イベント情報
     */
    public void logRejected(SecurityEventLogRecord record) {
        SECURITY_LOGGER.warn(format(record, REJECTED_RESULT));
    }

    private String format(SecurityEventLogRecord record, String result) {
        LoginUserContext loginUserContext = record.loginUserContext();
        return "requestId=" + valueOrEmpty(MDC.get(MDC_REQUEST_ID))
                + " userId=" + userId(loginUserContext)
                + " companyId=" + companyId(loginUserContext)
                + " actorType=" + actorType(loginUserContext)
                + " authorities=" + authorities(loginUserContext)
                + " eventType=" + valueOrEmpty(record.eventType())
                + " result=" + result
                + " reasonCode=" + valueOrEmpty(record.reasonCode())
                + " exceptionType=" + valueOrEmpty(record.exceptionType());
    }

    private String userId(LoginUserContext loginUserContext) {
        if (loginUserContext == null) {
            return EMPTY_VALUE;
        }
        return valueOrEmpty(loginUserContext.userId());
    }

    private String companyId(LoginUserContext loginUserContext) {
        if (loginUserContext == null) {
            return EMPTY_VALUE;
        }
        return valueOrEmpty(loginUserContext.companyId());
    }

    private String actorType(LoginUserContext loginUserContext) {
        if (loginUserContext == null) {
            return EMPTY_VALUE;
        }
        return valueOrEmpty(loginUserContext.actorType());
    }

    private String authorities(LoginUserContext loginUserContext) {
        if (loginUserContext == null || loginUserContext.permissionSets().isEmpty()) {
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
