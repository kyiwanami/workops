package com.example.workops.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Spring MVCで発生したアプリ例外をkey=value形式で出力するロガー。
 *
 * <p>入力値や例外メッセージはログ本文へ出さず、requestId、status、path、例外種別だけを CloudWatch Logsで検索できる形式にする。
 */
@Component
public class ApplicationExceptionLogger {

  private static final Logger APPLICATION_LOGGER =
      LoggerFactory.getLogger("com.example.workops.application");
  private static final String EMPTY_VALUE = "-";
  private static final String MDC_REQUEST_ID = "requestId";

  /**
   * アプリ例外をERRORログへ出力する。
   *
   * @param record 出力対象のアプリ例外ログ情報
   * @param exception 発生した例外
   */
  public void log(ApplicationExceptionLogRecord record, Exception exception) {
    APPLICATION_LOGGER.error(format(record), exception);
  }

  private String format(ApplicationExceptionLogRecord record) {
    return "requestId="
        + requestId()
        + " eventType="
        + valueOrEmpty(record.eventType())
        + " status="
        + record.status()
        + " path="
        + valueOrEmpty(record.path())
        + " exceptionType="
        + valueOrEmpty(record.exceptionType());
  }

  public String requestId() {
    return valueOrEmpty(MDC.get(MDC_REQUEST_ID));
  }

  private String valueOrEmpty(String value) {
    if (value == null || value.isEmpty()) {
      return EMPTY_VALUE;
    }
    return value.replace('\r', '_').replace('\n', '_');
  }
}
