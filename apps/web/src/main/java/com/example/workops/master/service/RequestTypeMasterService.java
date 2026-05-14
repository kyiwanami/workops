package com.example.workops.master.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    private final CurrentUserProvider currentUserProvider;
    private final RequestTypeMasterMapper requestTypeMasterMapper;

    public RequestTypeMasterService(
            CurrentUserProvider currentUserProvider,
            RequestTypeMasterMapper requestTypeMasterMapper) {
        this.currentUserProvider = currentUserProvider;
        this.requestTypeMasterMapper = requestTypeMasterMapper;
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
        assertUniqueCodeForCreate(currentUser.companyId(), requestTypeMasterForm.code());

        requestTypeMasterMapper.insertRequestType(
                findRequestTypeMasterId(),
                currentUser.companyId(),
                requestTypeMasterForm.code(),
                requestTypeMasterForm.name(),
                requestTypeMasterForm.sortOrder(),
                currentUser.userId(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void update(Long id, RequestTypeMasterForm requestTypeMasterForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        requestTypeMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。"));

        requestTypeMasterMapper.updateActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                requestTypeMasterForm.name(),
                requestTypeMasterForm.sortOrder(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void delete(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        requestTypeMasterMapper.findActiveByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。"));

        requestTypeMasterMapper.logicalDeleteActiveByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void restore(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        requestTypeMasterMapper.findDeletedByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。"));

        requestTypeMasterMapper.restoreDeletedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
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

    private void assertUniqueCodeForCreate(Long companyId, String code) {
        if (requestTypeMasterMapper.existsCodeByCompanyId(companyId, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申請種別コードは既に使用されています。");
        }
    }
}
