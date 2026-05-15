package com.example.workops.request.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import com.example.workops.request.form.RequestForm;
import com.example.workops.request.form.RequestReviewForm;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(RequestCommandServiceTests.RequestCommandServiceTestConfig.class)
class RequestCommandServiceTests {

    private static final Long REQUEST_ID = 100L;
    private static final Long COMPANY_ID = 1L;
    private static final Long REQUESTER_USER_ID = 2L;
    private static final Long MANAGER_USER_ID = 3L;
    private static final Long REQUEST_TYPE_VALUE_ID = 10L;

    @Autowired
    private RequestCommandService requestCommandService;

    @Autowired
    private RequestMapper requestMapper;

    @Autowired
    private OperationLogger operationLogger;

    @BeforeEach
    void setUp() {
        reset(requestMapper, operationLogger);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void editorCanCreateDraft() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID)).thenReturn(true);
        when(requestMapper.findLastInsertId()).thenReturn(200L);

        Long createdId = requestCommandService.createDraft(new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", "申請内容"));

        assertThat(createdId).isEqualTo(200L);
        verify(requestMapper).insertDraft(
                COMPANY_ID,
                REQUESTER_USER_ID,
                null,
                REQUEST_TYPE_VALUE_ID,
                "購入申請",
                "申請内容",
                REQUESTER_USER_ID,
                REQUESTER_USER_ID);
        assertSuccessLog("REQUEST_CREATE", 200L, false);
    }

    @Test
    void requesterCanUpdateDraft() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT", null)));
        when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID)).thenReturn(true);

        requestCommandService.updateDraft(REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, null, "修理申請", "修理内容"));

        verify(requestMapper).updateDraftByIdAndCompanyId(
                REQUEST_ID,
                COMPANY_ID,
                REQUESTER_USER_ID,
                null,
                REQUEST_TYPE_VALUE_ID,
                "修理申請",
                "修理内容",
                REQUESTER_USER_ID);
        assertSuccessLog("REQUEST_UPDATE", REQUEST_ID, false);
    }

    @Test
    void requesterCanSubmitDraftAndSubmittedAtIsPassedToMapper() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT", "差戻し理由")));
        ArgumentCaptor<LocalDateTime> submittedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        requestCommandService.submitDraft(REQUEST_ID);

        verify(requestMapper).submitDraftByIdAndCompanyId(
                eq(REQUEST_ID),
                eq(COMPANY_ID),
                eq(REQUESTER_USER_ID),
                submittedAtCaptor.capture(),
                eq(REQUESTER_USER_ID));
        assertThat(submittedAtCaptor.getValue()).isNotNull();
        assertSuccessLog("REQUEST_SUBMIT", REQUEST_ID, false);
    }

    @Test
    void requesterCanWithdrawSubmitted() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED", null)));

        requestCommandService.withdrawSubmitted(REQUEST_ID);

        verify(requestMapper).withdrawSubmittedByIdAndCompanyId(
                REQUEST_ID,
                COMPANY_ID,
                REQUESTER_USER_ID,
                REQUESTER_USER_ID);
        assertSuccessLog("REQUEST_WITHDRAW", REQUEST_ID, false);
    }

    @Test
    void managerCanApproveSubmitted() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED", "既存コメント")));

        requestCommandService.approveSubmitted(REQUEST_ID);

        verify(requestMapper).approveSubmittedByIdAndCompanyId(REQUEST_ID, COMPANY_ID, MANAGER_USER_ID);
        assertSuccessLog("REQUEST_APPROVE", REQUEST_ID, false);
    }

    @Test
    void managerCanRejectSubmittedAndReviewCommentIsPassedToMapper() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED", null)));

        requestCommandService.rejectSubmitted(REQUEST_ID, new RequestReviewForm("却下理由"));

        verify(requestMapper).rejectSubmittedByIdAndCompanyId(
                REQUEST_ID,
                COMPANY_ID,
                "却下理由",
                MANAGER_USER_ID);
        assertSuccessLog("REQUEST_REJECT", REQUEST_ID, true);
    }

    @Test
    void managerCanRemandSubmittedAndReviewCommentIsPassedToMapper() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED", null)));

        requestCommandService.remandSubmitted(REQUEST_ID, new RequestReviewForm("差戻し理由"));

        verify(requestMapper).remandSubmittedByIdAndCompanyId(
                REQUEST_ID,
                COMPANY_ID,
                "差戻し理由",
                MANAGER_USER_ID);
        assertSuccessLog("REQUEST_REMAND", REQUEST_ID, true);
    }

    @Test
    void managerCanReviewOwnSubmittedRequest() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestMapper.findDetailByIdAndCompanyId(201L, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(201L, MANAGER_USER_ID, "SUBMITTED", null)));
        when(requestMapper.findDetailByIdAndCompanyId(202L, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(202L, MANAGER_USER_ID, "SUBMITTED", null)));
        when(requestMapper.findDetailByIdAndCompanyId(203L, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(203L, MANAGER_USER_ID, "SUBMITTED", null)));

        requestCommandService.approveSubmitted(201L);
        requestCommandService.rejectSubmitted(202L, new RequestReviewForm("却下理由"));
        requestCommandService.remandSubmitted(203L, new RequestReviewForm("差戻し理由"));

        verify(requestMapper).approveSubmittedByIdAndCompanyId(201L, COMPANY_ID, MANAGER_USER_ID);
        verify(requestMapper).rejectSubmittedByIdAndCompanyId(202L, COMPANY_ID, "却下理由", MANAGER_USER_ID);
        verify(requestMapper).remandSubmittedByIdAndCompanyId(203L, COMPANY_ID, "差戻し理由", MANAGER_USER_ID);
        assertSuccessLogs(
                List.of("REQUEST_APPROVE", "REQUEST_REJECT", "REQUEST_REMAND"),
                List.of(201L, 202L, 203L),
                List.of(false, true, true));
    }

    @Test
    void viewerCannotExecuteApplicantOperations() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));

        assertThatThrownBy(() -> requestCommandService.createDraft(new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestCommandService.updateDraft(REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestCommandService.submitDraft(REQUEST_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestCommandService.withdrawSubmitted(REQUEST_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(operationLogger);
    }

    @Test
    void editorCannotExecuteReviewOperations() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));

        assertThatThrownBy(() -> requestCommandService.approveSubmitted(REQUEST_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestCommandService.rejectSubmitted(REQUEST_ID, new RequestReviewForm("却下理由")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestCommandService.remandSubmitted(REQUEST_ID, new RequestReviewForm("差戻し理由")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(operationLogger);
    }

    @Test
    void deletedOrOtherCompanyRequestTypeIsRejectedForCreateAndUpdate() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT", null)));
        when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> requestCommandService.createDraft(new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> requestCommandService.updateDraft(REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(requestMapper, never()).insertDraft(
                COMPANY_ID,
                REQUESTER_USER_ID,
                null,
                REQUEST_TYPE_VALUE_ID,
                "購入申請",
                null,
                REQUESTER_USER_ID,
                REQUESTER_USER_ID);
        verify(requestMapper, never()).updateDraftByIdAndCompanyId(
                REQUEST_ID,
                COMPANY_ID,
                REQUESTER_USER_ID,
                null,
                REQUEST_TYPE_VALUE_ID,
                "購入申請",
                null,
                REQUESTER_USER_ID);
        assertRejectedLogs(
                List.of("REQUEST_CREATE", "REQUEST_UPDATE"),
                Arrays.asList(null, REQUEST_ID),
                List.of("INVALID_REQUEST_TYPE", "INVALID_REQUEST_TYPE"),
                List.of("ResponseStatusException", "ResponseStatusException"));
    }

    @Test
    void deletedOrOtherCompanyAssetIsRejectedForCreateAndUpdate() {
        Long assetId = 300L;
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT", null)));
        when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID)).thenReturn(true);
        when(requestMapper.existsSelectableAssetByIdAndCompanyId(assetId, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> requestCommandService.createDraft(new RequestForm(REQUEST_TYPE_VALUE_ID, assetId, "購入申請", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> requestCommandService.updateDraft(REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, assetId, "購入申請", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertRejectedLogs(
                List.of("REQUEST_CREATE", "REQUEST_UPDATE"),
                Arrays.asList(null, REQUEST_ID),
                List.of("INVALID_ASSET", "INVALID_ASSET"),
                List.of("ResponseStatusException", "ResponseStatusException"));
    }

    @Test
    void otherUserCannotUpdateDraft() {
        signIn(3L, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT", null)));

        assertThatThrownBy(() -> requestCommandService.updateDraft(REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", null)))
                .isInstanceOf(AccessDeniedException.class);
        assertRejectedLog("REQUEST_UPDATE", REQUEST_ID, "REQUESTER_MISMATCH", "AccessDeniedException");
    }

    @Test
    void otherUserCannotWithdrawSubmitted() {
        signIn(3L, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED", null)));

        assertThatThrownBy(() -> requestCommandService.withdrawSubmitted(REQUEST_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertRejectedLog("REQUEST_WITHDRAW", REQUEST_ID, "REQUESTER_MISMATCH", "AccessDeniedException");
    }

    @Test
    void cannotSubmitNonDraft() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "APPROVED", null)));

        assertThatThrownBy(() -> requestCommandService.submitDraft(REQUEST_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertRejectedLog("REQUEST_SUBMIT", REQUEST_ID, "STATUS_MISMATCH", "AccessDeniedException");
    }

    @Test
    void cannotWithdrawNonSubmitted() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT", null)));

        assertThatThrownBy(() -> requestCommandService.withdrawSubmitted(REQUEST_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertRejectedLog("REQUEST_WITHDRAW", REQUEST_ID, "STATUS_MISMATCH", "AccessDeniedException");
    }

    @Test
    void cannotReviewNonSubmitted() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestMapper.findDetailByIdAndCompanyId(201L, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(201L, REQUESTER_USER_ID, "DRAFT", null)));
        when(requestMapper.findDetailByIdAndCompanyId(202L, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(202L, REQUESTER_USER_ID, "APPROVED", null)));
        when(requestMapper.findDetailByIdAndCompanyId(203L, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(203L, REQUESTER_USER_ID, "WITHDRAWN", null)));

        assertThatThrownBy(() -> requestCommandService.approveSubmitted(201L))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestCommandService.rejectSubmitted(202L, new RequestReviewForm("却下理由")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestCommandService.remandSubmitted(203L, new RequestReviewForm("差戻し理由")))
                .isInstanceOf(AccessDeniedException.class);
        assertRejectedLogs(
                List.of("REQUEST_APPROVE", "REQUEST_REJECT", "REQUEST_REMAND"),
                List.of(201L, 202L, 203L),
                List.of("STATUS_MISMATCH", "STATUS_MISMATCH", "STATUS_MISMATCH"),
                List.of("AccessDeniedException", "AccessDeniedException", "AccessDeniedException"));
    }

    @Test
    void otherCompanyRequestReturnsNotFoundForApplicantOperations() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestCommandService.updateDraft(REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> requestCommandService.submitDraft(REQUEST_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> requestCommandService.withdrawSubmitted(REQUEST_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertRejectedLogs(
                List.of("REQUEST_UPDATE", "REQUEST_SUBMIT", "REQUEST_WITHDRAW"),
                List.of(REQUEST_ID, REQUEST_ID, REQUEST_ID),
                List.of("COMPANY_MISMATCH", "COMPANY_MISMATCH", "COMPANY_MISMATCH"),
                List.of("ResponseStatusException", "ResponseStatusException", "ResponseStatusException"));
    }

    @Test
    void otherCompanyRequestReturnsNotFoundForReviewOperations() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestMapper.findDetailByIdAndCompanyId(201L, COMPANY_ID)).thenReturn(Optional.empty());
        when(requestMapper.findDetailByIdAndCompanyId(202L, COMPANY_ID)).thenReturn(Optional.empty());
        when(requestMapper.findDetailByIdAndCompanyId(203L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestCommandService.approveSubmitted(201L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> requestCommandService.rejectSubmitted(202L, new RequestReviewForm("却下理由")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> requestCommandService.remandSubmitted(203L, new RequestReviewForm("差戻し理由")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertRejectedLogs(
                List.of("REQUEST_APPROVE", "REQUEST_REJECT", "REQUEST_REMAND"),
                List.of(201L, 202L, 203L),
                List.of("COMPANY_MISMATCH", "COMPANY_MISMATCH", "COMPANY_MISMATCH"),
                List.of("ResponseStatusException", "ResponseStatusException", "ResponseStatusException"));
    }

    private void assertSuccessLog(String operation, Long targetId, boolean reasonCommentPresent) {
        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogger).logSuccess(captor.capture());

        OperationLogRecord record = captor.getValue();
        assertOperationLogRecord(record, operation, targetId, null, reasonCommentPresent, null);
    }

    private void assertSuccessLogs(List<String> operations, List<Long> targetIds, List<Boolean> reasonCommentPresentValues) {
        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogger, times(operations.size())).logSuccess(captor.capture());

        List<OperationLogRecord> records = captor.getAllValues();
        for (int index = 0; index < operations.size(); index++) {
            assertOperationLogRecord(
                    records.get(index),
                    operations.get(index),
                    targetIds.get(index),
                    null,
                    reasonCommentPresentValues.get(index),
                    null);
        }
    }

    private void assertRejectedLog(String operation, Long targetId, String reasonCode, String exceptionType) {
        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogger).logRejected(captor.capture());

        OperationLogRecord record = captor.getValue();
        assertOperationLogRecord(record, operation, targetId, reasonCode, false, exceptionType);
    }

    private void assertRejectedLogs(
            List<String> operations,
            List<Long> targetIds,
            List<String> reasonCodes,
            List<String> exceptionTypes) {
        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogger, times(operations.size())).logRejected(captor.capture());

        List<OperationLogRecord> records = captor.getAllValues();
        for (int index = 0; index < operations.size(); index++) {
            assertOperationLogRecord(
                    records.get(index),
                    operations.get(index),
                    targetIds.get(index),
                    reasonCodes.get(index),
                    false,
                    exceptionTypes.get(index));
        }
    }

    private void assertOperationLogRecord(
            OperationLogRecord record,
            String operation,
            Long targetId,
            String reasonCode,
            boolean reasonCommentPresent,
            String exceptionType) {
        assertThat(record.loginUserContext().userId()).isNotNull();
        assertThat(record.targetType()).isEqualTo("REQUEST");
        assertThat(record.operation()).isEqualTo(operation);
        assertThat(record.targetId()).isEqualTo(targetId);
        assertThat(record.reasonCode()).isEqualTo(reasonCode);
        assertThat(record.reasonCommentPresent()).isEqualTo(reasonCommentPresent);
        assertThat(record.exceptionType()).isEqualTo(exceptionType);
    }

    private void signIn(Long userId, Long companyId, PermissionSetContext permissionSet) {
        LoginUserContext loginUserContext = new LoginUserContext(
                userId,
                "test-user",
                "test-user@example.local",
                "TENANT",
                companyId,
                List.of(permissionSet));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loginUserContext,
                null,
                List.of(new SimpleGrantedAuthority(permissionSet.code()))));
    }

    private PermissionSetContext permission(String code, String name) {
        return new PermissionSetContext(code, name);
    }

    private RequestDetail requestDetail(Long id, Long requesterUserId, String statusCode, String reviewComment) {
        return new RequestDetail(
                id,
                requesterUserId,
                null,
                null,
                null,
                null,
                "申請者",
                REQUEST_TYPE_VALUE_ID,
                "EQUIPMENT_PURCHASE",
                "備品購入申請",
                false,
                statusCode,
                statusCode,
                "申請件名",
                "申請内容",
                reviewComment,
                null,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    @Configuration
    @EnableMethodSecurity
    static class RequestCommandServiceTestConfig {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new CurrentUserProvider();
        }

        @Bean
        RequestMapper requestMapper() {
            return mock(RequestMapper.class);
        }

        @Bean
        OperationLogger operationLogger() {
            return mock(OperationLogger.class);
        }

        @Bean
        RequestCommandService requestCommandService(
                CurrentUserProvider currentUserProvider,
                RequestMapper requestMapper,
                OperationLogger operationLogger) {
            return new RequestCommandService(currentUserProvider, requestMapper, operationLogger);
        }
    }
}
