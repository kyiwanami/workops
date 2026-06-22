package com.example.workops.admin.department.form;

import com.example.workops.admin.department.model.DepartmentListItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 部署登録・編集フォーム入力を保持する。 */
public record DepartmentForm(
    @NotBlank(message = "部署コードを入力してください。") @Size(max = 50, message = "部署コードは50文字以内で入力してください。")
        String code,
    @NotBlank(message = "部署名を入力してください。") @Size(max = 100, message = "部署名は100文字以内で入力してください。")
        String name) {

  public static DepartmentForm empty() {
    return new DepartmentForm(null, null);
  }

  public static DepartmentForm from(DepartmentListItem department) {
    return new DepartmentForm(department.code(), department.name());
  }
}
