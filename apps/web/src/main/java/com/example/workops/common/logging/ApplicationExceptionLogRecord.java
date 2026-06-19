package com.example.workops.common.logging;

/**
 * アプリ例外ログに出力するHTTP request由来の最小イベント情報。
 *
 * @param eventType アプリ例外イベント種別
 * @param status HTTP status
 * @param path query stringを含まないrequest path
 * @param exceptionType 例外種別
 */
public record ApplicationExceptionLogRecord(
        String eventType,
        int status,
        String path,
        String exceptionType) {
}
