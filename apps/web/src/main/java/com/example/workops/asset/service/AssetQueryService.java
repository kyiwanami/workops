package com.example.workops.asset.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.asset.form.AssetSearchForm;
import com.example.workops.asset.mapper.AssetMapper;
import com.example.workops.asset.model.AssetCategoryOption;
import com.example.workops.asset.model.AssetDepartmentOption;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.asset.model.AssetListItem;
import com.example.workops.asset.model.AssetStatusOption;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetCode;

/**
 * 資産カタログの参照系ユースケースと画面操作可否を扱うService。
 *
 * <p>参照系の会社境界は、現在ユーザーの {@code companyId} をMapper条件へ渡して守る。
 * {@code can...} メソッドは画面表示用の補助判定であり、更新系の最終判定は
 * {@link AssetCommandService} 側で再度実施する。</p>
 */
@Service
public class AssetQueryService {

    private final CurrentUserProvider currentUserProvider;
    private final AssetMapper assetMapper;

    public AssetQueryService(CurrentUserProvider currentUserProvider, AssetMapper assetMapper) {
        this.currentUserProvider = currentUserProvider;
        this.assetMapper = assetMapper;
    }

    /**
     * 現在ユーザーの会社に属する資産一覧を検索条件付きで取得する。
     *
     * @param assetSearchForm 資産一覧の検索条件
     * @return 会社境界で絞り込まれた資産一覧
     */
    public List<AssetListItem> findList(AssetSearchForm assetSearchForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findListByCompanyIdAndSearchForm(currentUser.companyId(), assetSearchForm);
    }

    /**
     * 現在ユーザーの会社に属する資産詳細を取得する。
     *
     * @param id 資産ID
     * @return 資産詳細
     * @throws ResponseStatusException 対象資産が存在しない、または他社資産の場合
     */
    public AssetDetail findDetail(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
    }

    /**
     * 資産登録・編集で選択できる未削除の資産分類を取得する。
     *
     * @return 現在ユーザーの会社に属する資産分類選択肢
     */
    public List<AssetCategoryOption> findAssetCategoryOptions() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findAssetCategoryOptionsByCompanyId(currentUser.companyId());
    }

    /**
     * 資産登録・編集で任意に選択できる未削除部署を取得する。
     *
     * @return 現在ユーザーの会社に属する部署選択肢
     */
    public List<AssetDepartmentOption> findDepartmentOptions() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findDepartmentOptionsByCompanyId(currentUser.companyId());
    }

    /**
     * 資産登録・編集・ステータス変更で選択できる資産ステータスを取得する。
     *
     * @return 共通マスタ由来の資産ステータス選択肢
     */
    public List<AssetStatusOption> findStatusOptions() {
        return assetMapper.findStatusOptions();
    }

    /**
     * 現在ユーザーが資産を登録できるか判定する。
     *
     * @return {@code TENANT_EDITOR} または {@code TENANT_MANAGER} を持つ場合はtrue
     */
    public boolean canCreateAsset() {
        return currentUserProvider.currentUser()
                .map(this::hasAssetWritePermission)
                .orElse(false);
    }

    /**
     * 資産詳細画面で編集操作を表示してよいか判定する。
     *
     * @param assetDetail 判定対象の資産
     * @return 資産編集権限を持つ場合はtrue
     */
    public boolean canEditAsset(AssetDetail assetDetail) {
        return currentUserProvider.currentUser()
                .map(this::hasAssetWritePermission)
                .orElse(false);
    }

    /**
     * 資産詳細画面でステータス変更操作を表示してよいか判定する。
     *
     * @param assetDetail 判定対象の資産
     * @return 資産更新権限を持つ場合はtrue
     */
    public boolean canChangeStatus(AssetDetail assetDetail) {
        return currentUserProvider.currentUser()
                .map(this::hasAssetWritePermission)
                .orElse(false);
    }

    /**
     * 資産詳細画面で論理削除操作を表示してよいか判定する。
     *
     * @param assetDetail 判定対象の資産
     * @return {@code TENANT_MANAGER} を持つ場合はtrue
     */
    public boolean canDeleteAsset(AssetDetail assetDetail) {
        return currentUserProvider.currentUser()
                .map(this::hasAssetDeletePermission)
                .orElse(false);
    }

    private boolean hasAssetWritePermission(LoginUserContext currentUser) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> PermissionSetCode.TENANT_EDITOR.name().equals(permissionSet.code())
                        || PermissionSetCode.TENANT_MANAGER.name().equals(permissionSet.code()));
    }

    private boolean hasAssetDeletePermission(LoginUserContext currentUser) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> PermissionSetCode.TENANT_MANAGER.name().equals(permissionSet.code()));
    }
}
