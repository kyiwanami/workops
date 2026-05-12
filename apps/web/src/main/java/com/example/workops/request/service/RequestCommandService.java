package com.example.workops.request.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    private final CurrentUserProvider currentUserProvider;
    private final RequestMapper requestMapper;

    public RequestCommandService(CurrentUserProvider currentUserProvider, RequestMapper requestMapper) {
        this.currentUserProvider = currentUserProvider;
        this.requestMapper = requestMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public RequestDetail findDraftForEdit(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertEditableDraft(requestDetail, currentUser);
        return requestDetail;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public Long createDraft(RequestForm requestForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assertSelectableProcessType(requestForm.processTypeCode());
        assertSelectableAsset(requestForm.assetId(), currentUser.companyId());

        requestMapper.insertDraft(
                currentUser.companyId(),
                currentUser.userId(),
                requestForm.assetId(),
                requestForm.processTypeCode(),
                requestForm.title(),
                requestForm.content(),
                currentUser.userId(),
                currentUser.userId());
        return requestMapper.findLastInsertId();
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void updateDraft(Long id, RequestForm requestForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertEditableDraft(requestDetail, currentUser);
        assertSelectableProcessType(requestForm.processTypeCode());
        assertSelectableAsset(requestForm.assetId(), currentUser.companyId());

        requestMapper.updateDraftByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                requestForm.assetId(),
                requestForm.processTypeCode(),
                requestForm.title(),
                requestForm.content(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void submitDraft(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertSubmittableDraft(requestDetail, currentUser);

        requestMapper.submitDraftByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                LocalDateTime.now(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public void withdrawSubmitted(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertWithdrawableSubmitted(requestDetail, currentUser);

        requestMapper.withdrawSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                currentUser.userId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public RequestDetail findSubmittedForReject(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRejectableSubmitted(requestDetail);
        return requestDetail;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public RequestDetail findSubmittedForRemand(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRemandableSubmitted(requestDetail);
        return requestDetail;
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void approveSubmitted(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertApprovableSubmitted(requestDetail);

        requestMapper.approveSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void rejectSubmitted(Long id, RequestReviewForm requestReviewForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRejectableSubmitted(requestDetail);

        requestMapper.rejectSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                requestReviewForm.reviewComment(),
                currentUser.userId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void remandSubmitted(Long id, RequestReviewForm requestReviewForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertRemandableSubmitted(requestDetail);

        requestMapper.remandSubmittedByIdAndCompanyId(
                id,
                currentUser.companyId(),
                requestReviewForm.reviewComment(),
                currentUser.userId());
    }

    private void assertEditableDraft(RequestDetail requestDetail, LoginUserContext currentUser) {
        if (!Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                || !DRAFT_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は編集できません。");
        }
    }

    private void assertSubmittableDraft(RequestDetail requestDetail, LoginUserContext currentUser) {
        if (!Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                || !DRAFT_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は提出できません。");
        }
    }

    private void assertWithdrawableSubmitted(RequestDetail requestDetail, LoginUserContext currentUser) {
        if (!Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                || !SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は取下げできません。");
        }
    }

    private void assertApprovableSubmitted(RequestDetail requestDetail) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は承認できません。");
        }
    }

    private void assertRejectableSubmitted(RequestDetail requestDetail) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は却下できません。");
        }
    }

    private void assertRemandableSubmitted(RequestDetail requestDetail) {
        if (!SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode())) {
            throw new AccessDeniedException("この申請は差戻しできません。");
        }
    }

    private void assertSelectableProcessType(String processTypeCode) {
        if (!requestMapper.existsProcessTypeCode(processTypeCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "処理タイプが不正です。");
        }
    }

    private void assertSelectableAsset(Long assetId, Long companyId) {
        if (assetId != null && !requestMapper.existsSelectableAssetByIdAndCompanyId(assetId, companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産が不正です。");
        }
    }
}
