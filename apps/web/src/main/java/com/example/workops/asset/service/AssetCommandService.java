package com.example.workops.asset.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.asset.form.AssetForm;
import com.example.workops.asset.form.AssetStatusForm;
import com.example.workops.asset.mapper.AssetMapper;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;

/**
 * 資産カタログの登録・編集を実行するService。
 */
@Service
public class AssetCommandService {

    private final CurrentUserProvider currentUserProvider;
    private final AssetMapper assetMapper;

    public AssetCommandService(CurrentUserProvider currentUserProvider, AssetMapper assetMapper) {
        this.currentUserProvider = currentUserProvider;
        this.assetMapper = assetMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public AssetDetail findAssetForEdit(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public AssetDetail findAssetForStatusChange(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public Long createAsset(AssetForm assetForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assertSelectableAssetCategory(assetForm.assetCategoryValueId(), currentUser.companyId());
        assertSelectableDepartment(assetForm.departmentId(), currentUser.companyId());
        assertSelectableStatus(assetForm.statusCode());
        assertUniqueCodeForCreate(assetForm.code(), currentUser.companyId());

        assetMapper.insertAsset(
                currentUser.companyId(),
                assetForm.assetCategoryValueId(),
                assetForm.departmentId(),
                assetForm.code(),
                assetForm.name(),
                assetForm.statusCode(),
                assetForm.note(),
                currentUser.userId(),
                currentUser.userId());
        return assetMapper.findLastInsertId();
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void updateAsset(Long id, AssetForm assetForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assetMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
        assertSelectableAssetCategory(assetForm.assetCategoryValueId(), currentUser.companyId());
        assertSelectableDepartment(assetForm.departmentId(), currentUser.companyId());
        assertSelectableStatus(assetForm.statusCode());
        assertUniqueCodeForUpdate(id, assetForm.code(), currentUser.companyId());

        assetMapper.updateAssetByIdAndCompanyId(
                id,
                currentUser.companyId(),
                assetForm.assetCategoryValueId(),
                assetForm.departmentId(),
                assetForm.code(),
                assetForm.name(),
                assetForm.statusCode(),
                assetForm.note(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void updateStatus(Long id, AssetStatusForm assetStatusForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assetMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
        assertSelectableStatus(assetStatusForm.statusCode());

        assetMapper.updateAssetStatusByIdAndCompanyId(
                id,
                currentUser.companyId(),
                assetStatusForm.statusCode(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void deleteAsset(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assetMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));

        assetMapper.logicalDeleteAssetByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public boolean isDuplicateCodeForCreate(String code) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.existsAssetCodeByCompanyId(code, currentUser.companyId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public boolean isDuplicateCodeForUpdate(Long id, String code) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetMapper.existsOtherAssetCodeByCompanyId(id, code, currentUser.companyId());
    }

    private void assertSelectableAssetCategory(Long assetCategoryValueId, Long companyId) {
        if (!assetMapper.existsAssetCategoryByIdAndCompanyId(assetCategoryValueId, companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産カテゴリが不正です。");
        }
    }

    private void assertSelectableDepartment(Long departmentId, Long companyId) {
        if (departmentId != null && !assetMapper.existsDepartmentByIdAndCompanyId(departmentId, companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理部署が不正です。");
        }
    }

    private void assertSelectableStatus(String statusCode) {
        if (!assetMapper.existsStatusCode(statusCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産ステータスが不正です。");
        }
    }

    private void assertUniqueCodeForCreate(String code, Long companyId) {
        if (assetMapper.existsAssetCodeByCompanyId(code, companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産コードは既に使用されています。");
        }
    }

    private void assertUniqueCodeForUpdate(Long id, String code, Long companyId) {
        if (assetMapper.existsOtherAssetCodeByCompanyId(id, code, companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産コードは既に使用されています。");
        }
    }
}
