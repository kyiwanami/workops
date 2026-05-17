package com.example.workops.admin.user.form;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 作成済みユーザーの編集可能項目を保持するフォーム。
 */
public record UserEditForm(
        @NotBlank(message = "表示名を入力してください。")
        @Size(max = 100, message = "表示名は100文字以内で入力してください。")
        String name,
        @NotBlank(message = "emailを入力してください。")
        @Email(message = "email形式が不正です。")
        @Size(max = 255, message = "emailは255文字以内で入力してください。")
        String email,
        Long departmentId,
        List<String> permissionSetCodes) {
}
