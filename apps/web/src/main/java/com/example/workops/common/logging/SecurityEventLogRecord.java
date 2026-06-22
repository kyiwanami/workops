package com.example.workops.common.logging;

import com.example.workops.common.security.LoginUserContext;

/**
 * 認証・認可ログに出力するDB由来ID中心のイベント情報。
 *
 * @param loginUserContext DB突合後の現在ユーザー。DB突合前または突合失敗時はnull
 * @param eventType 認証・認可イベント種別
 * @param reasonCode 拒否・失敗理由コード。成功時はnull
 * @param exceptionType 例外種別。例外がない場合はnull
 */
public record SecurityEventLogRecord(
    LoginUserContext loginUserContext, String eventType, String reasonCode, String exceptionType) {}
