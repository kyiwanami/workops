package com.example.workops.common.logging;

import com.example.workops.common.security.LoginUserContext;

/**
 * 業務操作ログに出力するID中心の操作情報。
 *
 * @param loginUserContext 操作者を表すDB由来の現在ユーザー
 * @param operation 操作コード
 * @param targetType 操作対象種別
 * @param targetId 操作対象ID。作成前の拒否など対象IDがない場合はnull
 * @param reasonCode 拒否理由コード。成功時はnull
 * @param reasonCommentPresent 却下・差戻しなどの理由コメントが入力されている場合はtrue
 * @param exceptionType 拒否時に呼び出し側へ返す例外種別。成功時はnull
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
