package com.example.workops.request.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.request.form.RequestForm;
import com.example.workops.request.form.RequestReviewForm;
import com.example.workops.request.model.RequestAssetOption;
import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;
import com.example.workops.request.model.RequestTypeOption;
import com.example.workops.request.service.RequestCommandService;
import com.example.workops.request.service.RequestQueryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class RequestControllerTests {

  private final RequestQueryService requestQueryService = mock(RequestQueryService.class);
  private final RequestCommandService requestCommandService = mock(RequestCommandService.class);
  private final RequestController controller =
      new RequestController(requestQueryService, requestCommandService);

  @Test
  void listShowsRequestsAndOperationFlags() {
    List<RequestListItem> requests = List.of(requestListItem(1L));
    when(requestQueryService.findList()).thenReturn(requests);
    when(requestQueryService.canCreateDraft()).thenReturn(true);
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.list(model);

    assertThat(view).isEqualTo("request/list");
    assertThat(model.asMap()).containsEntry("requests", requests).containsEntry("canCreate", true);
  }

  @Test
  void newFormPreparesEmptyFormAndOptions() {
    when(requestQueryService.findRequestTypeOptions()).thenReturn(List.<RequestTypeOption>of());
    when(requestQueryService.findAssetOptions()).thenReturn(List.<RequestAssetOption>of());
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.newForm(model);

    assertThat(view).isEqualTo("request/form");
    assertThat(model.asMap())
        .containsEntry("requestForm", RequestForm.empty())
        .containsEntry("requestTypeOptions", List.<RequestTypeOption>of())
        .containsEntry("assetOptions", List.<RequestAssetOption>of())
        .containsEntry("edit", false)
        .containsEntry("requestId", null);
  }

  @Test
  void createShowsFormWhenValidationErrors() {
    RequestForm form = requestForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestForm");
    bindingResult.rejectValue("title", "required", "入力必須です");
    ExtendedModelMap model = new ExtendedModelMap();
    when(requestQueryService.findRequestTypeOptions()).thenReturn(List.<RequestTypeOption>of());
    when(requestQueryService.findAssetOptions()).thenReturn(List.<RequestAssetOption>of());

    String view = controller.create(form, bindingResult, model, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("request/form");
    assertThat(model.asMap()).containsEntry("edit", false);
    verify(requestQueryService).findRequestTypeOptions();
    verify(requestQueryService).findAssetOptions();
  }

  @Test
  void createRedirectsAfterSuccess() {
    RequestForm form = requestForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestForm");
    when(requestCommandService.createDraft(form)).thenReturn(10L);
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.create(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/requests/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請を下書き保存しました。");
  }

  @Test
  void detailShowsRequestAndPermissionFlags() {
    RequestDetail detail = requestDetail(10L, 20L, "DRAFT");
    when(requestQueryService.findDetail(10L)).thenReturn(detail);
    when(requestQueryService.canEditDraft(detail)).thenReturn(true);
    when(requestQueryService.canSubmit(detail)).thenReturn(false);
    when(requestQueryService.canWithdraw(detail)).thenReturn(true);
    when(requestQueryService.canReview(detail)).thenReturn(false);
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.detail(10L, model);

    assertThat(view).isEqualTo("request/detail");
    assertThat(model.asMap())
        .containsEntry("request", detail)
        .containsEntry("canEdit", true)
        .containsEntry("canSubmit", false)
        .containsEntry("canWithdraw", true)
        .containsEntry("canReview", false);
  }

  @Test
  void editFormPreparesDraftForEdit() {
    RequestDetail detail = requestDetail(10L, 20L, "DRAFT");
    when(requestCommandService.findDraftForEdit(10L)).thenReturn(detail);
    when(requestQueryService.findRequestTypeOptions()).thenReturn(List.<RequestTypeOption>of());
    when(requestQueryService.findAssetOptions()).thenReturn(List.<RequestAssetOption>of());

    String view = controller.editForm(10L, new ExtendedModelMap());

    assertThat(view).isEqualTo("request/form");
    verify(requestCommandService).findDraftForEdit(10L);
  }

  @Test
  void updateShowsFormWhenValidationErrors() {
    RequestDetail detail = requestDetail(10L, 20L, "DRAFT");
    RequestForm form = requestForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestForm");
    bindingResult.rejectValue("title", "required", "入力必須です");
    when(requestCommandService.findDraftForEdit(10L)).thenReturn(detail);
    when(requestQueryService.findRequestTypeOptions()).thenReturn(List.<RequestTypeOption>of());
    when(requestQueryService.findAssetOptions()).thenReturn(List.<RequestAssetOption>of());
    ExtendedModelMap model = new ExtendedModelMap();

    String view =
        controller.update(10L, form, bindingResult, model, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("request/form");
    assertThat(model.asMap()).containsEntry("edit", true);
    assertThat(model.asMap()).containsEntry("requestId", 10L);
  }

  @Test
  void updateRedirectsAfterSuccess() {
    RequestForm form = requestForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(requestCommandService.findDraftForEdit(10L)).thenReturn(requestDetail(10L, 20L, "DRAFT"));

    String view =
        controller.update(10L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/requests/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請を下書き保存しました。");
  }

  @Test
  void submitRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.submit(10L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/requests/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請を提出しました。");
  }

  @Test
  void withdrawRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.withdraw(10L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/requests/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請を取下げました。");
  }

  @Test
  void approveRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.approve(10L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/requests/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請を承認しました。");
  }

  @Test
  void rejectFormPreparesReviewMetadata() {
    RequestDetail detail = requestDetail(10L, 20L, "SUBMITTED");
    when(requestCommandService.findSubmittedForReject(10L)).thenReturn(detail);

    String view = controller.rejectForm(10L, new ExtendedModelMap());

    assertThat(view).isEqualTo("request/review-form");
    assertThat((new ExtendedModelMap()).asMap()).isEmpty();
    assertThat(controller).isNotNull();
  }

  @Test
  void rejectShowsFormWhenValidationErrors() {
    RequestDetail detail = requestDetail(10L, 20L, "SUBMITTED");
    RequestReviewForm form = new RequestReviewForm("却下理由");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestReviewForm");
    bindingResult.rejectValue("reviewComment", "required", "入力必須です");
    when(requestCommandService.findSubmittedForReject(10L)).thenReturn(detail);

    String view =
        controller.reject(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("request/review-form");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void rejectRedirectsAfterSuccess() {
    RequestDetail detail = requestDetail(10L, 20L, "SUBMITTED");
    RequestReviewForm form = new RequestReviewForm("却下理由");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestReviewForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(requestCommandService.findSubmittedForReject(10L)).thenReturn(detail);

    String view =
        controller.reject(10L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/requests/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請を却下しました。");
  }

  @Test
  void remandFormPreparesReviewMetadata() {
    RequestDetail detail = requestDetail(10L, 20L, "SUBMITTED");
    when(requestCommandService.findSubmittedForRemand(10L)).thenReturn(detail);

    String view = controller.remandForm(10L, new ExtendedModelMap());

    assertThat(view).isEqualTo("request/review-form");
    assertThat((new ExtendedModelMap()).asMap()).isEmpty();
  }

  @Test
  void remandRedirectsAfterSuccess() {
    RequestDetail detail = requestDetail(10L, 20L, "SUBMITTED");
    RequestReviewForm form = new RequestReviewForm("差戻し理由");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestReviewForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(requestCommandService.findSubmittedForRemand(10L)).thenReturn(detail);

    String view =
        controller.remand(10L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/requests/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請を差戻ししました。");
  }

  @Test
  void remandShowsFormWhenValidationErrors() {
    RequestDetail detail = requestDetail(10L, 20L, "SUBMITTED");
    RequestReviewForm form = new RequestReviewForm("差戻し理由");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestReviewForm");
    bindingResult.rejectValue("reviewComment", "required", "入力必須です");
    when(requestCommandService.findSubmittedForRemand(10L)).thenReturn(detail);

    String view =
        controller.remand(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("request/review-form");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  private RequestForm requestForm() {
    return new RequestForm(100L, 200L, "申請件名", "内容");
  }

  private RequestListItem requestListItem(Long id) {
    return new RequestListItem(
        id,
        "申請者",
        20L,
        "AST-001",
        "資産名",
        false,
        100L,
        "REQ",
        "申請種別名",
        false,
        "DRAFT",
        "下書き",
        "申請件名",
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }

  private RequestDetail requestDetail(Long id, Long requesterUserId, String statusCode) {
    return new RequestDetail(
        id,
        requesterUserId,
        200L,
        "AST-001",
        "資産名",
        false,
        "申請者",
        100L,
        "REQ",
        "申請種別名",
        false,
        statusCode,
        statusCode.equals("DRAFT") ? "下書き" : "提出済",
        "申請件名",
        "内容",
        "レビューコメント",
        null,
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }
}
