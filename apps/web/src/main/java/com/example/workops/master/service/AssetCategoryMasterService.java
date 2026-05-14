package com.example.workops.master.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.master.form.AssetCategoryMasterForm;
import com.example.workops.master.form.AssetCategoryMasterSearchForm;
import com.example.workops.master.mapper.AssetCategoryMasterMapper;
import com.example.workops.master.model.AssetCategoryMasterDetail;
import com.example.workops.master.model.AssetCategoryMasterListItem;

/**
 * 資産分類マスタの管理ユースケースを扱うService。
 */
@Service
public class AssetCategoryMasterService {

    private final CurrentUserProvider currentUserProvider;
    private final AssetCategoryMasterMapper assetCategoryMasterMapper;

    public AssetCategoryMasterService(
            CurrentUserProvider currentUserProvider,
            AssetCategoryMasterMapper assetCategoryMasterMapper) {
        this.currentUserProvider = currentUserProvider;
        this.assetCategoryMasterMapper = assetCategoryMasterMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public List<AssetCategoryMasterListItem> findList(AssetCategoryMasterSearchForm assetCategoryMasterSearchForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetCategoryMasterMapper.findListByCompanyIdAndSearchForm(
                currentUser.companyId(),
                assetCategoryMasterSearchForm);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public AssetCategoryMasterDetail findForEdit(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetCategoryMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産分類が見つかりません。"));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void create(AssetCategoryMasterForm assetCategoryMasterForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assertUniqueCodeForCreate(currentUser.companyId(), assetCategoryMasterForm.code());

        assetCategoryMasterMapper.insertAssetCategory(
                findAssetCategoryMasterId(),
                currentUser.companyId(),
                assetCategoryMasterForm.code(),
                assetCategoryMasterForm.name(),
                assetCategoryMasterForm.sortOrder(),
                currentUser.userId(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void update(Long id, AssetCategoryMasterForm assetCategoryMasterForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assetCategoryMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産分類が見つかりません。"));

        assetCategoryMasterMapper.updateActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                assetCategoryMasterForm.name(),
                assetCategoryMasterForm.sortOrder(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void delete(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assetCategoryMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産分類が見つかりません。"));

        assetCategoryMasterMapper.logicalDeleteActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void restore(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assetCategoryMasterMapper.findDeletedByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産分類が見つかりません。"));

        assetCategoryMasterMapper.restoreDeletedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public boolean isDuplicateCodeForCreate(String code) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return assetCategoryMasterMapper.existsCodeByCompanyId(currentUser.companyId(), code);
    }

    private Long findAssetCategoryMasterId() {
        return assetCategoryMasterMapper.findAssetCategoryMasterId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "資産分類マスタ種別が見つかりません。"));
    }

    private void assertUniqueCodeForCreate(Long companyId, String code) {
        if (assetCategoryMasterMapper.existsCodeByCompanyId(companyId, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産分類コードは既に使用されています。");
        }
    }
}
