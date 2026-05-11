package com.example.workops.request.model;

/**
 * 申請作成・編集フォームで選択できる処理タイプ。
 */
public record RequestProcessTypeOption(
        String code,
        String name) {
}
