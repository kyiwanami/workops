package com.example.workops.admin.company.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PLATFORM_ADMINによる会社作成フォーム入力を保持する。
 */
public record CompanyForm(
        @NotBlank(message = "会社コードを入力してください。")
        @Size(max = 50, message = "会社コードは50文字以内で入力してください。")
        String code,
        @NotBlank(message = "会社名を入力してください。")
        @Size(max = 100, message = "会社名は100文字以内で入力してください。")
        String name) {

    public static CompanyForm empty() {
        return new CompanyForm(null, null);
    }
}
