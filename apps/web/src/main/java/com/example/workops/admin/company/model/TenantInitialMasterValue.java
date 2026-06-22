package com.example.workops.admin.company.model;

/** 会社作成時に投入する会社別汎用マスタ初期値を表す。 */
public record TenantInitialMasterValue(
    String genericMasterCode, String code, String name, Integer sortOrder) {}
