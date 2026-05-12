package com.example.workops.asset.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
 * 資産カタログの参照系ユースケースを扱うService。
 */
@Service
public class AssetQueryService {

    private final CurrentUserProvider currentUserProvider;
    private final AssetMapper assetMapper;

    public AssetQueryService(CurrentUserProvider currentUserProvider, AssetMapper assetMapper) {
        this.currentUserProvider = currentUserProvider;
        this.assetMapper = assetMapper;
    }

    public List<AssetListItem> findList() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findListByCompanyId(currentUser.companyId());
    }

    public AssetDetail findDetail(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
    }

    public List<AssetCategoryOption> findAssetCategoryOptions() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findAssetCategoryOptionsByCompanyId(currentUser.companyId());
    }

    public List<AssetDepartmentOption> findDepartmentOptions() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findDepartmentOptionsByCompanyId(currentUser.companyId());
    }

    public List<AssetStatusOption> findStatusOptions() {
        return assetMapper.findStatusOptions();
    }

    public boolean canCreateAsset() {
        return currentUserProvider.currentUser()
                .map(this::hasAssetWritePermission)
                .orElse(false);
    }

    public boolean canEditAsset(AssetDetail assetDetail) {
        return currentUserProvider.currentUser()
                .map(this::hasAssetWritePermission)
                .orElse(false);
    }

    private boolean hasAssetWritePermission(LoginUserContext currentUser) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> PermissionSetCode.TENANT_EDITOR.name().equals(permissionSet.code())
                        || PermissionSetCode.TENANT_MANAGER.name().equals(permissionSet.code()));
    }
}
