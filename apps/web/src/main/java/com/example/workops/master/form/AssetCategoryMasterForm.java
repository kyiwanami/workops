package com.example.workops.master.form;

import com.example.workops.master.model.AssetCategoryMasterDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 資産分類マスタの登録・編集フォーム入力を保持する。 */
public record AssetCategoryMasterForm(
    @NotBlank(message = "資産分類コードを入力してください。") @Size(max = 50, message = "資産分類コードは50文字以内で入力してください。")
        String code,
    @NotBlank(message = "資産分類名を入力してください。") @Size(max = 100, message = "資産分類名は100文字以内で入力してください。")
        String name,
    @NotNull(message = "表示順を入力してください。") Integer sortOrder) {

  public static AssetCategoryMasterForm empty() {
    return new AssetCategoryMasterForm(null, null, null);
  }

  public static AssetCategoryMasterForm from(AssetCategoryMasterDetail assetCategoryMasterDetail) {
    return new AssetCategoryMasterForm(
        assetCategoryMasterDetail.code(),
        assetCategoryMasterDetail.name(),
        assetCategoryMasterDetail.sortOrder());
  }
}
