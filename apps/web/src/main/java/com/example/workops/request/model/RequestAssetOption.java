package com.example.workops.request.model;

/**
 * 申請フォームで選択できる未削除資産。
 */
public record RequestAssetOption(
        Long id,
        String code,
        String name) {
}
