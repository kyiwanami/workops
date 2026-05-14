package com.example.workops.master.form;

import com.example.workops.master.model.RequestTypeMasterDetail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 申請種別マスタの登録・編集フォーム入力を保持する。
 */
public record RequestTypeMasterForm(
        @NotBlank(message = "申請種別コードを入力してください。")
        @Size(max = 50, message = "申請種別コードは50文字以内で入力してください。")
        String code,
        @NotBlank(message = "申請種別名を入力してください。")
        @Size(max = 100, message = "申請種別名は100文字以内で入力してください。")
        String name,
        @NotNull(message = "表示順を入力してください。")
        Integer sortOrder) {

    public static RequestTypeMasterForm empty() {
        return new RequestTypeMasterForm(null, null, null);
    }

    public static RequestTypeMasterForm from(RequestTypeMasterDetail requestTypeMasterDetail) {
        return new RequestTypeMasterForm(
                requestTypeMasterDetail.code(),
                requestTypeMasterDetail.name(),
                requestTypeMasterDetail.sortOrder());
    }
}
