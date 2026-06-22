package com.example.workops.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import com.example.workops.request.form.RequestForm;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestDetail;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

@SpringJUnitConfig(RequestAssetLinkServiceTests.RequestAssetLinkServiceTestConfig.class)
class RequestAssetLinkServiceTests {

  private static final Long REQUEST_ID = 100L;
  private static final Long COMPANY_ID = 1L;
  private static final Long REQUESTER_USER_ID = 2L;
  private static final Long MANAGER_USER_ID = 3L;
  private static final Long ASSET_ID = 10L;
  private static final Long REQUEST_TYPE_VALUE_ID = 10L;

  @Autowired private RequestCommandService requestCommandService;

  @Autowired private RequestMapper requestMapper;

  @Autowired private OperationLogger operationLogger;

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
  void createDraftAllowsNoAsset() {
    signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
    when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID))
        .thenReturn(true);
    when(requestMapper.findLastInsertId()).thenReturn(200L);

    Long createdId =
        requestCommandService.createDraft(
            new RequestForm(REQUEST_TYPE_VALUE_ID, null, "購入申請", "申請内容"));

    assertThat(createdId).isEqualTo(200L);
    verify(requestMapper, never()).existsSelectableAssetByIdAndCompanyId(null, COMPANY_ID);
    verify(requestMapper)
        .insertDraft(
            COMPANY_ID,
            REQUESTER_USER_ID,
            null,
            REQUEST_TYPE_VALUE_ID,
            "購入申請",
            "申請内容",
            REQUESTER_USER_ID,
            REQUESTER_USER_ID);
  }

  @Test
  void createDraftAllowsSelectableAsset() {
    signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
    when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID))
        .thenReturn(true);
    when(requestMapper.existsSelectableAssetByIdAndCompanyId(ASSET_ID, COMPANY_ID))
        .thenReturn(true);
    when(requestMapper.findLastInsertId()).thenReturn(201L);

    Long createdId =
        requestCommandService.createDraft(
            new RequestForm(REQUEST_TYPE_VALUE_ID, ASSET_ID, "購入申請", "申請内容"));

    assertThat(createdId).isEqualTo(201L);
    verify(requestMapper)
        .insertDraft(
            COMPANY_ID,
            REQUESTER_USER_ID,
            ASSET_ID,
            REQUEST_TYPE_VALUE_ID,
            "購入申請",
            "申請内容",
            REQUESTER_USER_ID,
            REQUESTER_USER_ID);
  }

  @Test
  void updateDraftAllowsChangingAsset() {
    signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
    when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
        .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT")));
    when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID))
        .thenReturn(true);
    when(requestMapper.existsSelectableAssetByIdAndCompanyId(ASSET_ID, COMPANY_ID))
        .thenReturn(true);

    requestCommandService.updateDraft(
        REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, ASSET_ID, "修理申請", "修理内容"));

    verify(requestMapper)
        .updateDraftByIdAndCompanyId(
            REQUEST_ID,
            COMPANY_ID,
            REQUESTER_USER_ID,
            ASSET_ID,
            REQUEST_TYPE_VALUE_ID,
            "修理申請",
            "修理内容",
            REQUESTER_USER_ID);
  }

  @Test
  void updateDraftAllowsClearingAsset() {
    signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
    when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
        .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT")));
    when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID))
        .thenReturn(true);

    requestCommandService.updateDraft(
        REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, null, "修理申請", "修理内容"));

    verify(requestMapper, never()).existsSelectableAssetByIdAndCompanyId(null, COMPANY_ID);
    verify(requestMapper)
        .updateDraftByIdAndCompanyId(
            REQUEST_ID,
            COMPANY_ID,
            REQUESTER_USER_ID,
            null,
            REQUEST_TYPE_VALUE_ID,
            "修理申請",
            "修理内容",
            REQUESTER_USER_ID);
  }

  @Test
  void createDraftRejectsOtherCompanyOrDeletedAsset() {
    signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
    when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID))
        .thenReturn(true);
    when(requestMapper.existsSelectableAssetByIdAndCompanyId(ASSET_ID, COMPANY_ID))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                requestCommandService.createDraft(
                    new RequestForm(REQUEST_TYPE_VALUE_ID, ASSET_ID, "購入申請", "申請内容")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void updateDraftRejectsOtherCompanyOrDeletedAsset() {
    signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
    when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
        .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT")));
    when(requestMapper.existsRequestTypeByIdAndCompanyId(REQUEST_TYPE_VALUE_ID, COMPANY_ID))
        .thenReturn(true);
    when(requestMapper.existsSelectableAssetByIdAndCompanyId(ASSET_ID, COMPANY_ID))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                requestCommandService.updateDraft(
                    REQUEST_ID, new RequestForm(REQUEST_TYPE_VALUE_ID, ASSET_ID, "修理申請", "修理内容")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void workflowOperationsDoNotValidateAssetLink() {
    signIn(REQUESTER_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
    when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
        .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "DRAFT")));

    requestCommandService.submitDraft(REQUEST_ID);

    verify(requestMapper, never()).existsSelectableAssetByIdAndCompanyId(ASSET_ID, COMPANY_ID);
  }

  @Test
  void managerReviewOperationsDoNotValidateAssetLink() {
    signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
    when(requestMapper.findDetailByIdAndCompanyId(REQUEST_ID, COMPANY_ID))
        .thenReturn(Optional.of(requestDetail(REQUEST_ID, REQUESTER_USER_ID, "SUBMITTED")));

    requestCommandService.approveSubmitted(REQUEST_ID);

    verify(requestMapper, never()).existsSelectableAssetByIdAndCompanyId(ASSET_ID, COMPANY_ID);
  }

  private void signIn(Long userId, Long companyId, PermissionSetContext permissionSet) {
    LoginUserContext loginUserContext =
        new LoginUserContext(
            userId,
            "test-user",
            "test-user@example.local",
            "TENANT",
            companyId,
            List.of(permissionSet));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                loginUserContext, null, List.of(new SimpleGrantedAuthority(permissionSet.code()))));
  }

  private PermissionSetContext permission(String code, String name) {
    return new PermissionSetContext(code, name);
  }

  private RequestDetail requestDetail(Long id, Long requesterUserId, String statusCode) {
    return new RequestDetail(
        id,
        requesterUserId,
        ASSET_ID,
        "KTHM-TEST-001",
        "テスト資産",
        false,
        "申請者",
        REQUEST_TYPE_VALUE_ID,
        "EQUIPMENT_PURCHASE",
        "備品購入申請",
        false,
        statusCode,
        statusCode,
        "申請件名",
        "申請内容",
        null,
        null,
        LocalDateTime.of(2026, 5, 1, 9, 0),
        LocalDateTime.of(2026, 5, 1, 9, 0));
  }

  @Configuration
  @EnableMethodSecurity
  static class RequestAssetLinkServiceTestConfig {

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
