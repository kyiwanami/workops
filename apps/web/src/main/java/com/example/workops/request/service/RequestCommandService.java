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
 * 申請の下書き作成、提出、レビュー操作を実行するService。
 *
 * <p>このServiceの更新系メソッドは、Spring Securityの権限判定に加えて、申請者本人条件、
 * 申請ステータス条件、会社境界、申請種別と資産の選択可否を検証する。会社境界はMapperの
 * {@code company_id} 条件で守り、主要な成功・拒否結果は業務操作ログへ出力する。</p>
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

    /**
     * DRAFTの申請を編集画面用に取得する。
     *
     * @param id 申請ID
     * @return 現在ユーザーが編集できるDRAFT申請
     * @throws AccessDeniedException 申請者本人でない、またはDRAFTでない場合
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public RequestDetail findDraftForEdit(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertEditableDraftForRead(requestDetail, currentUser);
        return requestDetail;
    }

    /**
     * 現在ユーザーの会社にDRAFT申請を作成する。
     *
     * @param requestForm 入力済みの申請フォーム
     * @return 作成した申請ID
     * @throws ResponseStatusException 申請種別または関連資産が現在ユーザーの会社で選択できない場合
     */
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

    /**
     * 現在ユーザー本人が作成したDRAFT申請を更新する。
     *
     * @param id 申請ID
     * @param requestForm 更新後の申請フォーム
     * @throws AccessDeniedException 申請者本人でない、またはDRAFTでない場合
     * @throws ResponseStatusException 申請種別または関連資産が現在ユーザーの会社で選択できない場合
     */
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

    /**
     * 現在ユーザー本人が作成したDRAFT申請を{@code SUBMITTED}へ提出する。
     *
     * @param id 申請ID
     * @throws AccessDeniedException 申請者本人でない、またはDRAFTでない場合
     */
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

    /**
     * 現在ユーザー本人が作成した{@code SUBMITTED}申請を{@code WITHDRAWN}へ取下げる。
     *
     * @param id 申請ID
     * @throws AccessDeniedException 申請者本人でない、またはSUBMITTEDでない場合
     */
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

    /**
     * TENANT_MANAGERが却下できる{@code SUBMITTED}申請を取得する。
     *
     * @param id 申請ID
     * @return 却下理由入力に使う申請詳細
     * @throws AccessDeniedException SUBMITTEDでない場合
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public RequestDetail findSubmittedForReject(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRejectableSubmittedForRead(requestDetail);
        return requestDetail;
    }

    /**
     * TENANT_MANAGERが差戻しできる{@code SUBMITTED}申請を取得する。
     *
     * @param id 申請ID
     * @return 差戻し理由入力に使う申請詳細
     * @throws AccessDeniedException SUBMITTEDでない場合
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public RequestDetail findSubmittedForRemand(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRemandableSubmittedForRead(requestDetail);
        return requestDetail;
    }

    /**
     * TENANT_MANAGERが{@code SUBMITTED}申請を{@code APPROVED}へ承認する。
     *
     * @param id 申請ID
     * @throws AccessDeniedException SUBMITTEDでない場合
     */
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

    /**
     * TENANT_MANAGERが{@code SUBMITTED}申請を{@code REJECTED}へ却下する。
     *
     * @param id 申請ID
     * @param requestReviewForm 却下理由を含むフォーム
     * @throws AccessDeniedException SUBMITTEDでない場合
     */
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

    /**
     * TENANT_MANAGERが{@code SUBMITTED}申請を{@code DRAFT}へ差戻す。
     *
     * @param id 申請ID
     * @param requestReviewForm 差戻し理由を含むフォーム
     * @throws AccessDeniedException SUBMITTEDでない場合
     */
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
