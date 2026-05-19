package com.example.workops.admin.company.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.workops.admin.company.model.CompanyDetail;

/**
 * PLATFORM_ADMINによる会社編集フォーム入力を保持する。
 */
public record CompanyEditForm(
        @NotBlank(message = "会社名を入力してください。")
        @Size(max = 100, message = "会社名は100文字以内で入力してください。")
        String name) {

    public static CompanyEditForm from(CompanyDetail company) {
        return new CompanyEditForm(company.name());
    }
}
