package com.example.workops.request.service;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.request.form.RequestForm;
import com.example.workops.request.mapper.RequestDraftInsertCommand;
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
    public RequestDetail findDraftForEdit(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertEditableDraft(requestDetail, currentUser);
        return requestDetail;
    }

    @Transactional
    public Long createDraft(RequestForm requestForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assertSelectableProcessType(requestForm.getProcessTypeCode());

        RequestDraftInsertCommand command = new RequestDraftInsertCommand(
                currentUser.companyId(),
                currentUser.userId(),
                requestForm.getProcessTypeCode(),
                requestForm.getTitle(),
                requestForm.getContent(),
                currentUser.userId(),
                currentUser.userId());
        requestMapper.insertDraft(command);
        return command.getId();
    }

    @Transactional
    public void updateDraft(Long id, RequestForm requestForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertEditableDraft(requestDetail, currentUser);
        assertSelectableProcessType(requestForm.getProcessTypeCode());

        int updatedCount = requestMapper.updateDraftByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                requestForm.getProcessTypeCode(),
                requestForm.getTitle(),
                requestForm.getContent(),
                currentUser.userId());
        if (updatedCount != 1) {
            throw new AccessDeniedException("この申請は編集できません。");
        }
    }

    @Transactional
    public void submitDraft(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        RequestDetail requestDetail = requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
        assertSubmittableDraft(requestDetail, currentUser);

        requestMapper.submitDraftByIdAndCompanyId(
                id,
                currentUser.companyId(),
                currentUser.userId(),
                currentUser.userId());
    }

    @Transactional
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

    private void assertSelectableProcessType(String processTypeCode) {
        if (!requestMapper.existsProcessTypeCode(processTypeCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "処理タイプが不正です。");
        }
    }
}
