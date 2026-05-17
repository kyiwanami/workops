package com.example.workops.admin.user.model;

/**
 * ユーザー作成画面で選択できる部署を表す。
 */
public record DepartmentSelectOption(
        Long id,
        Long companyId,
        String companyCode,
        String name) {
}
