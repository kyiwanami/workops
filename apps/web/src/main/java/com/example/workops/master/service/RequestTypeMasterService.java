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
import com.example.workops.master.form.RequestTypeMasterForm;
import com.example.workops.master.form.RequestTypeMasterSearchForm;
import com.example.workops.master.mapper.RequestTypeMasterMapper;
import com.example.workops.master.model.RequestTypeMasterDetail;
import com.example.workops.master.model.RequestTypeMasterListItem;

/**
 * 申請種別マスタの管理ユースケースを扱うService。
 */
@Service
public class RequestTypeMasterService {

    private static final String GENERIC_MASTER_VALUE_TARGET_TYPE = "GENERIC_MASTER_VALUE";
    private static final String OPERATION_REQUEST_TYPE_CREATE = "REQUEST_TYPE_CREATE";
    private static final String OPERATION_REQUEST_TYPE_UPDATE = "REQUEST_TYPE_UPDATE";
    private static final String OPERATION_REQUEST_TYPE_DELETE = "REQUEST_TYPE_DELETE";
    private static final String OPERATION_REQUEST_TYPE_RESTORE = "REQUEST_TYPE_RESTORE";
    private static final String REASON_DUPLICATE_MASTER_VALUE_CODE = "DUPLICATE_MASTER_VALUE_CODE";
    private static final String REASON_MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN = "MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN";
    private static final String REASON_MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED = "MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED";
    private static final String EXCEPTION_RESPONSE_STATUS = "ResponseStatusException";

    private final CurrentUserProvider currentUserProvider;
    private final RequestTypeMasterMapper requestTypeMasterMapper;
    private final OperationLogger operationLogger;

    public RequestTypeMasterService(
            CurrentUserProvider currentUserProvider,
            RequestTypeMasterMapper requestTypeMasterMapper,
            OperationLogger operationLogger) {
        this.currentUserProvider = currentUserProvider;
        this.requestTypeMasterMapper = requestTypeMasterMapper;
        this.operationLogger = operationLogger;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public List<RequestTypeMasterListItem> findList(RequestTypeMasterSearchForm requestTypeMasterSearchForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestTypeMasterMapper.findListByCompanyIdAndSearchForm(
                currentUser.companyId(),
                requestTypeMasterSearchForm);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public RequestTypeMasterDetail findForEdit(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestTypeMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。"));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void create(RequestTypeMasterForm requestTypeMasterForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assertUniqueCodeForCreate(currentUser, requestTypeMasterForm.code());

        requestTypeMasterMapper.insertRequestType(
                findRequestTypeMasterId(),
                currentUser.companyId(),
                requestTypeMasterForm.code(),
                requestTypeMasterForm.name(),
                requestTypeMasterForm.sortOrder(),
                currentUser.userId(),
                currentUser.userId());
        Long createdId = requestTypeMasterMapper.findLastInsertId();
        logSuccess(currentUser, OPERATION_REQUEST_TYPE_CREATE, createdId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void update(Long id, RequestTypeMasterForm requestTypeMasterForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        findActiveForOperation(id, currentUser, OPERATION_REQUEST_TYPE_UPDATE);

        requestTypeMasterMapper.updateActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                requestTypeMasterForm.name(),
                requestTypeMasterForm.sortOrder(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_TYPE_UPDATE, id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void delete(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        findActiveForOperation(id, currentUser, OPERATION_REQUEST_TYPE_DELETE);

        requestTypeMasterMapper.logicalDeleteActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_TYPE_DELETE, id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void restore(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        findDeletedForOperation(id, currentUser, OPERATION_REQUEST_TYPE_RESTORE);

        requestTypeMasterMapper.restoreDeletedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_TYPE_RESTORE, id);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public boolean isDuplicateCodeForCreate(String code) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestTypeMasterMapper.existsCodeByCompanyId(currentUser.companyId(), code);
    }

    private Long findRequestTypeMasterId() {
        return requestTypeMasterMapper.findRequestTypeMasterId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "申請種別マスタ種別が見つかりません。"));
    }

    private RequestTypeMasterDetail findActiveForOperation(Long id, LoginUserContext currentUser, String operation) {
        return requestTypeMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> {
                    logRejected(currentUser, operation, id, REASON_MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。");
                });
    }

    private RequestTypeMasterDetail findDeletedForOperation(Long id, LoginUserContext currentUser, String operation) {
        return requestTypeMasterMapper.findDeletedByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> {
                    logRejected(currentUser, operation, id, REASON_MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。");
                });
    }

    private void assertUniqueCodeForCreate(LoginUserContext currentUser, String code) {
        if (requestTypeMasterMapper.existsCodeByCompanyId(currentUser.companyId(), code)) {
            logRejected(currentUser, OPERATION_REQUEST_TYPE_CREATE, null, REASON_DUPLICATE_MASTER_VALUE_CODE);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申請種別コードは既に使用されています。");
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
