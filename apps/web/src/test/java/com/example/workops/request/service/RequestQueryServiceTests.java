package com.example.workops.request.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestAssetOption;
import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;
import com.example.workops.request.model.RequestProcessTypeOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestQueryServiceTests {

    private static final Long REQUEST_ID = 100L;
    private static final Long COMPANY_ID = 1L;
    private static final Long REQUESTER_USER_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;

    private RequestQueryService requestQueryService;
    private RequestMapper requestMapper;

    @BeforeEach
    void setUp() {
        requestMapper = mock(RequestMapper.class);
        requestQueryService = new RequestQueryService(new CurrentUserProvider(), requestMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findListUsesCurrentCompanyId() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));
        when(requestMapper.findListByCompanyId(COMPANY_ID)).thenReturn(List.of(requestListItem()));

        List<RequestListItem> requests = requestQueryService.findList();

        assertThat(requests).hasSize(1);
        verify(requestMapper).findListByCompanyId(COMPANY_ID);
    }

    @Test
    void findDetailReturnsDetailOrNotFound() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));
        when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT")));
        when(requestMapper.findDetailByIdAndCompanyId(999L, COMPANY_ID))
                .thenReturn(Optional.empty());

        RequestDetail requestDetail = requestQueryService.findDetail(REQUEST_ID);

        assertThat(requestDetail.id()).isEqualTo(REQUEST_ID);
        assertThatThrownBy(() -> requestQueryService.findDetail(999L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void optionQueriesUseMapper() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(requestMapper.findProcessTypeOptions())
                .thenReturn(List.of(new RequestProcessTypeOption("PURCHASE", "購入")));
        when(requestMapper.findAssetOptionsByCompanyId(COMPANY_ID))
                .thenReturn(List.of(new RequestAssetOption(1L, "KTHM-NB-001", "営業部ノートPC")));

        assertThat(requestQueryService.findProcessTypeOptions()).hasSize(1);
        assertThat(requestQueryService.findAssetOptions()).hasSize(1);
        verify(requestMapper).findProcessTypeOptions();
        verify(requestMapper).findAssetOptionsByCompanyId(COMPANY_ID);
    }

    @Test
    void viewerCannotOperateDraftOrReview() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));
        RequestDetail ownDraft = requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT");
        RequestDetail ownSubmitted = requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED");

        assertThat(requestQueryService.canCreateDraft()).isFalse();
        assertThat(requestQueryService.canEditDraft(ownDraft)).isFalse();
        assertThat(requestQueryService.canSubmit(ownDraft)).isFalse();
        assertThat(requestQueryService.canWithdraw(ownSubmitted)).isFalse();
        assertThat(requestQueryService.canReview(ownSubmitted)).isFalse();
    }

    @Test
    void editorCanOperateOwnDraftAndSubmittedWithdrawOnly() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        RequestDetail ownDraft = requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT");
        RequestDetail otherDraft = requestDetail(REQUEST_ID, OTHER_USER_ID, "DRAFT");
        RequestDetail ownSubmitted = requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED");

        assertThat(requestQueryService.canCreateDraft()).isTrue();
        assertThat(requestQueryService.canEditDraft(ownDraft)).isTrue();
        assertThat(requestQueryService.canEditDraft(otherDraft)).isFalse();
        assertThat(requestQueryService.canSubmit(ownDraft)).isTrue();
        assertThat(requestQueryService.canSubmit(ownSubmitted)).isFalse();
        assertThat(requestQueryService.canWithdraw(ownSubmitted)).isTrue();
        assertThat(requestQueryService.canWithdraw(ownDraft)).isFalse();
        assertThat(requestQueryService.canReview(ownSubmitted)).isFalse();
    }

    @Test
    void managerCanOperateOwnDraftAndReviewSubmitted() {
        signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        RequestDetail ownDraft = requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT");
        RequestDetail ownSubmitted = requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED");
        RequestDetail otherSubmitted = requestDetail(REQUEST_ID, OTHER_USER_ID, "SUBMITTED");
        RequestDetail approved = requestDetail(REQUEST_ID, OTHER_USER_ID, "APPROVED");

        assertThat(requestQueryService.canCreateDraft()).isTrue();
        assertThat(requestQueryService.canEditDraft(ownDraft)).isTrue();
        assertThat(requestQueryService.canSubmit(ownDraft)).isTrue();
        assertThat(requestQueryService.canWithdraw(ownSubmitted)).isTrue();
        assertThat(requestQueryService.canReview(ownSubmitted)).isTrue();
        assertThat(requestQueryService.canReview(otherSubmitted)).isTrue();
        assertThat(requestQueryService.canReview(approved)).isFalse();
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

    private RequestListItem requestListItem() {
        return new RequestListItem(
                REQUEST_ID,
                "申請者",
                1L,
                "KTHM-NB-001",
                "営業部ノートPC",
                false,
                "PURCHASE",
                "購入",
                "DRAFT",
                "下書き",
                "申請件名",
                null,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    private RequestDetail requestDetail(Long id, Long requesterUserId, String statusCode) {
        return new RequestDetail(
                id,
                requesterUserId,
                1L,
                "KTHM-NB-001",
                "営業部ノートPC",
                false,
                "申請者",
                "PURCHASE",
                "購入",
                statusCode,
                statusCode,
                "申請件名",
                "申請内容",
                null,
                null,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }
}
