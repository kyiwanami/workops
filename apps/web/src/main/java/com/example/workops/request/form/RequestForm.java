package com.example.workops.request.form;

import com.example.workops.request.model.RequestDetail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 申請の下書き作成・編集フォーム入力を保持する。
 */
public record RequestForm(
        @NotNull(message = "申請種別を選択してください。")
        Long requestTypeValueId,
        Long assetId,
        @NotBlank(message = "件名を入力してください。")
        @Size(max = 100, message = "件名は100文字以内で入力してください。")
        String title,
        @Size(max = 2000, message = "申請内容は2000文字以内で入力してください。")
        String content) {

    public static RequestForm empty() {
        return new RequestForm(null, null, null, null);
    }

    public static RequestForm from(RequestDetail requestDetail) {
        return new RequestForm(
                requestDetail.requestTypeValueId(),
                requestDetail.assetId(),
                requestDetail.title(),
                requestDetail.content());
    }
}
