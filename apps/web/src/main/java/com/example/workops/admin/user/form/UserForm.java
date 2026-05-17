package com.example.workops.admin.user.form;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PLATFORM_ADMINによるユーザー作成フォーム入力を保持する。
 */
public record UserForm(
        @NotBlank(message = "actor_typeを選択してください。")
        String actorType,
        Long companyId,
        Long departmentId,
        @NotBlank(message = "ユーザー名を入力してください。")
        @Size(max = 100, message = "ユーザー名は100文字以内で入力してください。")
        String username,
        @NotBlank(message = "表示名を入力してください。")
        @Size(max = 100, message = "表示名は100文字以内で入力してください。")
        String name,
        @NotBlank(message = "emailを入力してください。")
        @Email(message = "email形式が不正です。")
        @Size(max = 255, message = "emailは255文字以内で入力してください。")
        String email,
        List<String> permissionSetCodes) {

    public static UserForm empty() {
        return new UserForm("TENANT", null, null, null, null, null, List.of());
    }
}
