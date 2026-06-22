package com.example.workops.admin.company.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PLATFORM_ADMINによる会社作成フォーム入力を保持する。 */
public record CompanyForm(
    @NotBlank(message = "会社コードを入力してください。") @Size(max = 50, message = "会社コードは50文字以内で入力してください。")
        String code,
    @NotBlank(message = "会社名を入力してください。") @Size(max = 100, message = "会社名は100文字以内で入力してください。")
        String name,
    @NotBlank(message = "初期管理者のユーザー名を入力してください。")
        @Size(max = 100, message = "初期管理者のユーザー名は100文字以内で入力してください。")
        String initialTenantManagerUsername,
    @NotBlank(message = "初期管理者の表示名を入力してください。")
        @Size(max = 100, message = "初期管理者の表示名は100文字以内で入力してください。")
        String initialTenantManagerName,
    @NotBlank(message = "初期管理者のemailを入力してください。")
        @Email(message = "初期管理者のemail形式が不正です。")
        @Size(max = 255, message = "初期管理者のemailは255文字以内で入力してください。")
        String initialTenantManagerEmail) {

  public static CompanyForm empty() {
    return new CompanyForm(null, null, null, null, null);
  }
}
