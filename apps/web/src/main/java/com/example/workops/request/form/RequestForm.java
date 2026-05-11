package com.example.workops.request.form;

import com.example.workops.request.model.RequestDetail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 申請の下書き作成・編集フォーム入力を保持する。
 */
public class RequestForm {

    @NotBlank(message = "処理タイプを選択してください。")
    private String processTypeCode;

    @NotBlank(message = "件名を入力してください。")
    @Size(max = 100, message = "件名は100文字以内で入力してください。")
    private String title;

    @Size(max = 2000, message = "申請内容は2000文字以内で入力してください。")
    private String content;

    public static RequestForm from(RequestDetail requestDetail) {
        RequestForm form = new RequestForm();
        form.setProcessTypeCode(requestDetail.processTypeCode());
        form.setTitle(requestDetail.title());
        form.setContent(requestDetail.content());
        return form;
    }

    public String getProcessTypeCode() {
        return processTypeCode;
    }

    public void setProcessTypeCode(String processTypeCode) {
        this.processTypeCode = processTypeCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
