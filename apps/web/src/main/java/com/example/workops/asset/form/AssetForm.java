package com.example.workops.asset.form;

import com.example.workops.asset.model.AssetDetail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 資産登録・編集フォーム入力を保持する。
 */
public record AssetForm(
        @NotBlank(message = "資産コードを入力してください。")
        @Size(max = 50, message = "資産コードは50文字以内で入力してください。")
        String code,
        @NotBlank(message = "資産名を入力してください。")
        @Size(max = 100, message = "資産名は100文字以内で入力してください。")
        String name,
        @NotNull(message = "資産カテゴリを選択してください。")
        Long assetCategoryValueId,
        Long departmentId,
        @NotBlank(message = "資産ステータスを選択してください。")
        String statusCode,
        @Size(max = 1000, message = "備考は1000文字以内で入力してください。")
        String note) {

    public static AssetForm empty() {
        return new AssetForm(null, null, null, null, null, null);
    }

    public static AssetForm from(AssetDetail assetDetail) {
        return new AssetForm(
                assetDetail.code(),
                assetDetail.name(),
                assetDetail.assetCategoryValueId(),
                assetDetail.departmentId(),
                assetDetail.statusCode(),
                assetDetail.note());
    }
}
