package com.example.workops.master.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
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

    private static final String GENERIC_MASTER_VALUE_TARGET_TYPE = "GENERIC_MASTER_VALUE";
    private static final String OPERATION_ASSET_CATEGORY_CREATE = "ASSET_CATEGORY_CREATE";
    private static final String OPERATION_ASSET_CATEGORY_UPDATE = "ASSET_CATEGORY_UPDATE";
    private static final String OPERATION_ASSET_CATEGORY_DELETE = "ASSET_CATEGORY_DELETE";
    private static final String OPERATION_ASSET_CATEGORY_RESTORE = "ASSET_CATEGORY_RESTORE";
    private static final String REASON_DUPLICATE_MASTER_VALUE_CODE = "DUPLICATE_MASTER_VALUE_CODE";
    private static final String REASON_MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN = "MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN";
    private static final String REASON_MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED = "MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED";
    private static final String EXCEPTION_RESPONSE_STATUS = "ResponseStatusException";

    private final CurrentUserProvider currentUserProvider;
    private final AssetCategoryMasterMapper assetCategoryMasterMapper;
    private final OperationLogger operationLogger;

    public AssetCategoryMasterService(
            CurrentUserProvider currentUserProvider,
            AssetCategoryMasterMapper assetCategoryMasterMapper,
            OperationLogger operationLogger) {
        this.currentUserProvider = currentUserProvider;
        this.assetCategoryMasterMapper = assetCategoryMasterMapper;
        this.operationLogger = operationLogger;
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
        assertUniqueCodeForCreate(currentUser, assetCategoryMasterForm.code());

        assetCategoryMasterMapper.insertAssetCategory(
                findAssetCategoryMasterId(),
                currentUser.companyId(),
                assetCategoryMasterForm.code(),
                assetCategoryMasterForm.name(),
                assetCategoryMasterForm.sortOrder(),
                currentUser.userId(),
                currentUser.userId());
        Long createdId = assetCategoryMasterMapper.findLastInsertId();
        logSuccess(currentUser, OPERATION_ASSET_CATEGORY_CREATE, createdId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void update(Long id, AssetCategoryMasterForm assetCategoryMasterForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        findActiveForOperation(id, currentUser, OPERATION_ASSET_CATEGORY_UPDATE);

        assetCategoryMasterMapper.updateActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                assetCategoryMasterForm.name(),
                assetCategoryMasterForm.sortOrder(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_ASSET_CATEGORY_UPDATE, id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void delete(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        findActiveForOperation(id, currentUser, OPERATION_ASSET_CATEGORY_DELETE);

        assetCategoryMasterMapper.logicalDeleteActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_ASSET_CATEGORY_DELETE, id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void restore(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        findDeletedForOperation(id, currentUser, OPERATION_ASSET_CATEGORY_RESTORE);

        assetCategoryMasterMapper.restoreDeletedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_ASSET_CATEGORY_RESTORE, id);
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

    private AssetCategoryMasterDetail findActiveForOperation(Long id, LoginUserContext currentUser, String operation) {
        return assetCategoryMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> {
                    logRejected(currentUser, operation, id, REASON_MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "資産分類が見つかりません。");
                });
    }

    private AssetCategoryMasterDetail findDeletedForOperation(Long id, LoginUserContext currentUser, String operation) {
        return assetCategoryMasterMapper.findDeletedByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> {
                    logRejected(currentUser, operation, id, REASON_MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "資産分類が見つかりません。");
                });
    }

    private void assertUniqueCodeForCreate(LoginUserContext currentUser, String code) {
        if (assetCategoryMasterMapper.existsCodeByCompanyId(currentUser.companyId(), code)) {
            logRejected(currentUser, OPERATION_ASSET_CATEGORY_CREATE, null, REASON_DUPLICATE_MASTER_VALUE_CODE);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産分類コードは既に使用されています。");
        }
    }

    private void logSuccess(LoginUserContext currentUser, String operation, Long targetId) {
        operationLogger.logSuccess(new OperationLogRecord(
                currentUser,
                operation,
                GENERIC_MASTER_VALUE_TARGET_TYPE,
                targetId,
                null,
                false,
                null));
    }

    private void logRejected(LoginUserContext currentUser, String operation, Long targetId, String reasonCode) {
        operationLogger.logRejected(new OperationLogRecord(
                currentUser,
                operation,
                GENERIC_MASTER_VALUE_TARGET_TYPE,
                targetId,
                reasonCode,
                false,
                EXCEPTION_RESPONSE_STATUS));
    }
}
