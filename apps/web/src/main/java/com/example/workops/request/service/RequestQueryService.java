package com.example.workops.request.service;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetCode;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestAssetOption;
import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;
import com.example.workops.request.model.RequestProcessTypeOption;

/**
 * 申請管理の参照系ユースケースを扱うService。
 */
@Service
public class RequestQueryService {

    private static final String DRAFT_STATUS_CODE = "DRAFT";
    private static final String SUBMITTED_STATUS_CODE = "SUBMITTED";

    private final CurrentUserProvider currentUserProvider;
    private final RequestMapper requestMapper;

    public RequestQueryService(CurrentUserProvider currentUserProvider, RequestMapper requestMapper) {
        this.currentUserProvider = currentUserProvider;
        this.requestMapper = requestMapper;
    }

    public List<RequestListItem> findList() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findListByCompanyId(currentUser.companyId());
    }

    public RequestDetail findDetail(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
    }

    public List<RequestProcessTypeOption> findProcessTypeOptions() {
        return requestMapper.findProcessTypeOptions();
    }

    public List<RequestAssetOption> findAssetOptions() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findAssetOptionsByCompanyId(currentUser.companyId());
    }

    public boolean canCreateDraft() {
        return currentUserProvider.currentUser()
                .map(this::hasApplicantOperationPermission)
                .orElse(false);
    }

    public boolean canEditDraft(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasApplicantOperationPermission(currentUser)
                        && Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                        && DRAFT_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    public boolean canSubmit(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasApplicantOperationPermission(currentUser)
                        && Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                        && DRAFT_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    public boolean canWithdraw(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasApplicantOperationPermission(currentUser)
                        && Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                        && SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    public boolean canReview(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasManagerPermission(currentUser)
                        && SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    private boolean hasApplicantOperationPermission(LoginUserContext currentUser) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> PermissionSetCode.TENANT_EDITOR.name().equals(permissionSet.code())
                        || PermissionSetCode.TENANT_MANAGER.name().equals(permissionSet.code()));
    }

    private boolean hasManagerPermission(LoginUserContext currentUser) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> PermissionSetCode.TENANT_MANAGER.name().equals(permissionSet.code()));
    }
}
