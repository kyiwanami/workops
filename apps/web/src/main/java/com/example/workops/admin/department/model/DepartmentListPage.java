package com.example.workops.admin.department.model;

import java.util.List;

/**
 * 部署一覧画面に必要な会社情報と部署行を表す。
 */
public record DepartmentListPage(
        Long companyId,
        String companyCode,
        String companyName,
        Boolean showDeleted,
        List<DepartmentListItem> departments) {

    public DepartmentListPage {
        departments = List.copyOf(departments);
    }
}
