package com.example.workops.request.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.request.form.RequestForm;
import com.example.workops.request.form.RequestReviewForm;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestDetail;

/**
 * 申請の下書き作成・編集を実行するService。
 */
@Service
public class RequestCommandService {

    private static final String DRAFT_STATUS_CODE = "DRAFT";
    private static final String SUBMITTED_STATUS_CODE = "SUBMITTED";
    private static final String REQUEST_TARGET_TYPE = "REQUEST";
    private static final String OPERATION_REQUEST_CREATE = "REQUEST_CREATE";
    private static final String OPERATION_REQUEST_UPDATE = "REQUEST_UPDATE";
    private static final String OPERATION_REQUEST_SUBMIT = "REQUEST_SUBMIT";
    private static final String OPERATION_REQUEST_APPROVE = "REQUEST_APPROVE";
    private static final String OPERATION_REQUEST_REJECT = "REQUEST_REJECT";
    private static final String OPERATION_REQUEST_REMAND = "REQUEST_REMAND";
    private static final String OPERATION_REQUEST_WITHDRAW = "REQUEST_WITHDRAW";
    private static final String REASON_COMPANY_MISMATCH = "COMPANY_MISMATCH";
    private static final String REASON_REQUESTER_MISMATCH = "REQUESTER_MISMATCH";
    private static final String REASON_STATUS_MISMATCH = "STATUS_MISMATCH";
    private static final String REASON_INVALID_REQUEST_TYPE = "INVALID_REQUEST_TYPE";
    private static final String REASON_INVALID_ASSET = "INVALID_ASSET";
    private static final String EXCEPTION_ACCESS_DENIED = "AccessDeniedException";
    private static final String EXCEPTION_RESPONSE_STATUS = "ResponseStatusException";

    private final CurrentUserProvider currentUserProvider;
    private final RequestMapper requestMapper;
    private final OperationLogger operationLogger;

    public RequestCommandService(
            CurrentUserProvider currentUserProvider,
            RequestMapper requestMapper,
            OperationLogger operationLogger) {
        this.currentUserProvider = currentUserProvider;
        this.requestMapper = requestMapper;
        this.operationLogger = operationLogger;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public RequestDetail findDraftForEdit(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertEditableDraftForRead(requestDetail, currentUser);
        return requestDetail;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public Long createDraft(RequestForm requestForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assertSelectableRequestType(requestForm.requestTypeValueId(), currentUser, OPERATION_REQUEST_CREATE, null);
        assertSelectableAsset(requestForm.assetId(), currentUser, OPERATION_REQUEST_CREATE, null);

        requestMapper.insertDraft(
                currentUser.companyId(),
                currentUser.userId(),
                requestForm.assetId(),
                requestForm.requestTypeValueId(),
                requestForm.title(),
                requestForm.content(),
                currentUser.userId(),
                currentUser.userId());
        Long createdId = requestMapper.findLastInsertId();
        logSuccess(currentUser, OPERATION_REQUEST_CREATE, createdId, false);
        return createdId;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void updateDraft(Long id, RequestForm requestForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = findRequestForOperation(id, currentUser, OPERATION_REQUEST_UPDATE);
        assertEditableDraft(requestDetail, currentUser, OPERATION_REQUEST_UPDATE);
        assertSelectableRequestType(requestForm.requestTypeValueId(), currentUser, OPERATION_REQUEST_UPDATE, id);
        assertSelectableAsset(requestForm.assetId(), currentUser, OPERATION_REQUEST_UPDATE, id);

        requestMapper.updateDraftByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                requestForm.assetId(),
                requestForm.requestTypeValueId(),
                requestForm.title(),
                requestForm.content(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_UPDATE, id, false);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void submitDraft(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = findRequestForOperation(id, currentUser, OPERATION_REQUEST_SUBMIT);
        assertSubmittableDraft(requestDetail, currentUser, OPERATION_REQUEST_SUBMIT);

        requestMapper.submitDraftByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                LocalDateTime.now(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_SUBMIT, id, false);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void withdrawSubmitted(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = findRequestForOperation(id, currentUser, OPERATION_REQUEST_WITHDRAW);
        assertWithdrawableSubmitted(requestDetail, currentUser, OPERATION_REQUEST_WITHDRAW);

        requestMapper.withdrawSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_WITHDRAW, id, false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public RequestDetail findSubmittedForReject(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRejectableSubmittedForRead(requestDetail);
        return requestDetail;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public RequestDetail findSubmittedForRemand(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRemandableSubmittedForRead(requestDetail);
        return requestDetail;
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void approveSubmitted(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = findRequestForOperation(id, currentUser, OPERATION_REQUEST_APPROVE);
        assertApprovableSubmitted(requestDetail, currentUser, OPERATION_REQUEST_APPROVE);

        requestMapper.approveSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_APPROVE, id, false);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void rejectSubmitted(Long id, RequestReviewForm requestReviewForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = findRequestForOperation(id, currentUser, OPERATION_REQUEST_REJECT);
        assertRejectableSubmitted(requestDetail, currentUser, OPERATION_REQUEST_REJECT);

        requestMapper.rejectSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                requestReviewForm.reviewComment(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_REJECT, id, hasReviewComment(requestReviewForm));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void remandSubmitted(Long id, RequestReviewForm requestReviewForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = findRequestForOperation(id, currentUser, OPERATION_REQUEST_REMAND);
        assertRemandableSubmitted(requestDetail, currentUser, OPERATION_REQUEST_REMAND);

        requestMapper.remandSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                requestReviewForm.reviewComment(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_REQUEST_REMAND, id, hasReviewComment(requestReviewForm));
    }

    private RequestDetail findRequestForOperation(Long id, LoginUserContext currentUser, String operation) {
        return requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> {
                    logRejected(currentUser, operation, id, REASON_COMPANY_MISMATCH, false, EXCEPTION_RESPONSE_STATUS);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。");
                });
    }

    private void assertEditableDraftForRead(RequestDetail requestDetail, LoginUserContext currentUser) {
        if (!Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                || !DRAFT_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は編集できません。");
        }
    }

    private void assertRejectableSubmittedForRead(RequestDetail requestDetail) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は却下できません。");
        }
    }

    private void assertRemandableSubmittedForRead(RequestDetail requestDetail) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は差戻しできません。");
        }
    }

    private void assertEditableDraft(RequestDetail requestDetail, LoginUserContext currentUser, String operation) {
        if (!Objects.equals(requestDetail.requesterUserId(), currentUser.userId())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_REQUESTER_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は編集できません。");
        }
        if (!DRAFT_STATUS_CODE.equals(requestDetail.statusCode())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_STATUS_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は編集できません。");
        }
    }

    private void assertSubmittableDraft(RequestDetail requestDetail, LoginUserContext currentUser, String operation) {
        if (!Objects.equals(requestDetail.requesterUserId(), currentUser.userId())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_REQUESTER_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は提出できません。");
        }
        if (!DRAFT_STATUS_CODE.equals(requestDetail.statusCode())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_STATUS_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は提出できません。");
        }
    }

    private void assertWithdrawableSubmitted(RequestDetail requestDetail, LoginUserContext currentUser, String operation) {
        if (!Objects.equals(requestDetail.requesterUserId(), currentUser.userId())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_REQUESTER_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は取下げできません。");
        }
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_STATUS_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は取下げできません。");
        }
    }

    private void assertApprovableSubmitted(RequestDetail requestDetail, LoginUserContext currentUser, String operation) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_STATUS_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は承認できません。");
        }
    }

    private void assertRejectableSubmitted(RequestDetail requestDetail, LoginUserContext currentUser, String operation) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_STATUS_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は却下できません。");
        }
    }

    private void assertRemandableSubmitted(RequestDetail requestDetail, LoginUserContext currentUser, String operation) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            logRejected(currentUser, operation, requestDetail.id(), REASON_STATUS_MISMATCH, false, EXCEPTION_ACCESS_DENIED);
            throw new AccessDeniedException("この申請は差戻しできません。");
        }
    }

    private void assertSelectableRequestType(
            Long requestTypeValueId,
            LoginUserContext currentUser,
            String operation,
            Long targetId) {
        if (!requestMapper.existsRequestTypeByIdAndCompanyId(requestTypeValueId, currentUser.companyId())) {
            logRejected(currentUser, operation, targetId, REASON_INVALID_REQUEST_TYPE, false, EXCEPTION_RESPONSE_STATUS);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申請種別が不正です。");
        }
    }

    private void assertSelectableAsset(
            Long assetId,
            LoginUserContext currentUser,
            String operation,
            Long targetId) {
        if (assetId != null && !requestMapper.existsSelectableAssetByIdAndCompanyId(assetId, currentUser.companyId())) {
            logRejected(currentUser, operation, targetId, REASON_INVALID_ASSET, false, EXCEPTION_RESPONSE_STATUS);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産が不正です。");
        }
    }

    private void logSuccess(
            LoginUserContext currentUser,
            String operation,
            Long targetId,
            boolean reasonCommentPresent) {
        operationLogger.logSuccess(new OperationLogRecord(
                currentUser,
                operation,
                REQUEST_TARGET_TYPE,
                targetId,
                null,
                reasonCommentPresent,
                null));
    }

    private void logRejected(
            LoginUserContext currentUser,
            String operation,
            Long targetId,
            String reasonCode,
            boolean reasonCommentPresent,
            String exceptionType) {
        operationLogger.logRejected(new OperationLogRecord(
                currentUser,
                operation,
                REQUEST_TARGET_TYPE,
                targetId,
                reasonCode,
                reasonCommentPresent,
                exceptionType));
    }

    private boolean hasReviewComment(RequestReviewForm requestReviewForm) {
        return requestReviewForm.reviewComment() != null && !requestReviewForm.reviewComment().isEmpty();
    }
}
