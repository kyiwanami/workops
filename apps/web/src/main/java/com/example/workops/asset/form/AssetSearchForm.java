package com.example.workops.asset.form;

/**
 * 資産一覧の検索条件を保持する。
 */
public record AssetSearchForm(
        String assetCode,
        String assetName,
        Long assetCategoryValueId,
        Long departmentId,
        String statusCode) {
}
