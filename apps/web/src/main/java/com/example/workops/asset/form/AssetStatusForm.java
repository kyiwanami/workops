package com.example.workops.asset.form;

import com.example.workops.asset.model.AssetDetail;
import jakarta.validation.constraints.NotBlank;

/** 資産ステータス変更フォーム入力を保持する。 */
public record AssetStatusForm(@NotBlank(message = "資産ステータスを選択してください。") String statusCode) {

  public static AssetStatusForm from(AssetDetail assetDetail) {
    return new AssetStatusForm(assetDetail.statusCode());
  }
}
