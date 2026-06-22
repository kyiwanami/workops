package com.example.workops.common.logging;

import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 主要な更新系業務操作をkey=value形式で出力するロガー。
 *
 * <p>業務操作ログはRDBへ保存せず、Spring Boot標準のロギング基盤へ出力する。
 * 本文やコメントの内容は出さず、対象ID、操作名、拒否理由、コメント有無など調査に必要なID中心の値を出力する。
 */
@Component
public class OperationLogger {

  private static final Logger OPERATION_LOGGER =
      LoggerFactory.getLogger("com.example.workops.operation");
  private static final String EMPTY_VALUE = "-";
  private static final String MDC_REQUEST_ID = "requestId";
  private static final String SUCCESS_RESULT = "SUCCESS";
  private static final String REJECTED_RESULT = "REJECTED";

  /**
   * 業務操作の成功結果をINFOログへ出力する。
   *
   * @param record 出力対象の業務操作ログ情報
   */
  public void logSuccess(OperationLogRecord record) {
    OPERATION_LOGGER.info(format(record, SUCCESS_RESULT));
  }

  /**
   * 業務ルールや入力条件による想定内拒否をWARNログへ出力する。
   *
   * @param record 出力対象の業務操作ログ情報
   */
  public void logRejected(OperationLogRecord record) {
    OPERATION_LOGGER.warn(format(record, REJECTED_RESULT));
  }

  private String format(OperationLogRecord record, String result) {
    LoginUserContext loginUserContext = record.loginUserContext();
    return "requestId="
        + valueOrEmpty(MDC.get(MDC_REQUEST_ID))
        + " userId="
        + valueOrEmpty(loginUserContext.userId())
        + " companyId="
        + valueOrEmpty(loginUserContext.companyId())
        + " actorType="
        + valueOrEmpty(loginUserContext.actorType())
        + " authorities="
        + authorities(loginUserContext)
        + " operation="
        + valueOrEmpty(record.operation())
        + " targetType="
        + valueOrEmpty(record.targetType())
        + " targetId="
        + valueOrEmpty(record.targetId())
        + " result="
        + result
        + " reasonCode="
        + valueOrEmpty(record.reasonCode())
        + " reasonCommentPresent="
        + record.reasonCommentPresent()
        + " exceptionType="
        + valueOrEmpty(record.exceptionType());
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
    return value.replace('\r', '_').replace('\n', '_');
  }
}
