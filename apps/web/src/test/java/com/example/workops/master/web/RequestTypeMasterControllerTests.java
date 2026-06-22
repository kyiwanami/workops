package com.example.workops.master.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.master.form.RequestTypeMasterForm;
import com.example.workops.master.form.RequestTypeMasterSearchForm;
import com.example.workops.master.model.RequestTypeMasterDetail;
import com.example.workops.master.model.RequestTypeMasterListItem;
import com.example.workops.master.service.RequestTypeMasterService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class RequestTypeMasterControllerTests {

  private final RequestTypeMasterService requestTypeMasterService =
      mock(RequestTypeMasterService.class);
  private final RequestTypeMasterController controller =
      new RequestTypeMasterController(requestTypeMasterService);

  @Test
  void listShowsRequestTypes() {
    RequestTypeMasterSearchForm searchForm = new RequestTypeMasterSearchForm(false);
    when(requestTypeMasterService.findList(searchForm))
        .thenReturn(List.of(requestTypeMasterListItem(1L, "REQ01")));
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.list(searchForm, model);

    assertThat(view).isEqualTo("master/request-type-list");
    assertThat(model.asMap())
        .containsEntry("requestTypes", List.of(requestTypeMasterListItem(1L, "REQ01")));
  }

  @Test
  void newFormShowsEmptyForm() {
    String view = controller.newForm(new ExtendedModelMap());

    assertThat(view).isEqualTo("master/request-type-form");
  }

  @Test
  void createShowsFormWhenValidationErrors() {
    RequestTypeMasterForm form = new RequestTypeMasterForm("REQ01", "備品購入", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestTypeMasterForm");
    bindingResult.rejectValue("code", "required", "入力必須");

    String view =
        controller.create(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("master/request-type-form");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void createRejectsDuplicateCode() {
    RequestTypeMasterForm form = new RequestTypeMasterForm("REQ01", "備品購入", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestTypeMasterForm");
    when(requestTypeMasterService.isDuplicateCodeForCreate(form.code())).thenReturn(true);

    String view =
        controller.create(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("master/request-type-form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
    assertThat(bindingResult.getFieldError("code").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("code").getDefaultMessage())
        .isEqualTo("申請種別コードは既に使用されています。");
  }

  @Test
  void createRejectsDuplicateCodeAsErrorResponse() {
    RequestTypeMasterForm form = new RequestTypeMasterForm("REQ01", "備品購入", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestTypeMasterForm");
    when(requestTypeMasterService.isDuplicateCodeForCreate(form.code())).thenReturn(true);

    String view =
        controller.create(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("master/request-type-form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
  }

  @Test
  void createRedirectsAfterSuccess() {
    RequestTypeMasterForm form = new RequestTypeMasterForm("REQ01", "備品購入", 1);
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.create(
            form,
            new BeanPropertyBindingResult(form, "requestTypeMasterForm"),
            new ExtendedModelMap(),
            redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/request-types");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請種別を登録しました。");
  }

  @Test
  void editFormShowsFormModel() {
    when(requestTypeMasterService.findForEdit(1L)).thenReturn(requestTypeMasterDetail(1L, "REQ01"));
    String view = controller.editForm(1L, new ExtendedModelMap());

    assertThat(view).isEqualTo("master/request-type-form");
    verify(requestTypeMasterService).findForEdit(1L);
  }

  @Test
  void updateShowsFormWhenValidationErrors() {
    RequestTypeMasterForm form = new RequestTypeMasterForm("REQ01", "備品購入", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestTypeMasterForm");
    bindingResult.rejectValue("name", "required", "入力必須");
    when(requestTypeMasterService.findForEdit(1L)).thenReturn(requestTypeMasterDetail(1L, "REQ01"));

    String view =
        controller.update(
            1L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("master/request-type-form");
    assertThat(bindingResult.hasErrors()).isTrue();
    verify(requestTypeMasterService, never()).update(1L, form);
  }

  @Test
  void updateRedirectsAfterSuccess() {
    RequestTypeMasterForm form = new RequestTypeMasterForm("REQ01", "備品購入", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "requestTypeMasterForm");
    when(requestTypeMasterService.findForEdit(1L)).thenReturn(requestTypeMasterDetail(1L, "REQ01"));
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.update(1L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/request-types");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請種別を更新しました。");
  }

  @Test
  void deleteRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.delete(1L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/request-types");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請種別を削除しました。");
  }

  @Test
  void restoreRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.restore(1L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/request-types");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "申請種別を復活しました。");
  }

  private RequestTypeMasterListItem requestTypeMasterListItem(Long id, String code) {
    return new RequestTypeMasterListItem(
        id,
        code,
        "申請種別",
        1,
        false,
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }

  private RequestTypeMasterDetail requestTypeMasterDetail(Long id, String code) {
    return new RequestTypeMasterDetail(
        id,
        code,
        "申請種別",
        1,
        false,
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }
}
