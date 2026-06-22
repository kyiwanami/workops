package com.example.workops.admin.company.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.admin.company.form.CompanyEditForm;
import com.example.workops.admin.company.form.CompanyForm;
import com.example.workops.admin.company.form.CompanySearchForm;
import com.example.workops.admin.company.model.CompanyDetail;
import com.example.workops.admin.company.model.CompanyListItem;
import com.example.workops.admin.company.service.CompanyAdminService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class CompanyAdminControllerTests {

  private final CompanyAdminService companyAdminService = mock(CompanyAdminService.class);
  private final CompanyAdminController controller = new CompanyAdminController(companyAdminService);

  @Test
  void listShowsCompanyList() {
    List<CompanyListItem> companies = List.of(companyListItem());
    CompanySearchForm searchForm = new CompanySearchForm(false);
    when(companyAdminService.findCompanies(searchForm)).thenReturn(companies);

    String view = controller.list(searchForm, new ExtendedModelMap());

    assertThat(view).isEqualTo("admin/company/company-list");
    assertThat(((ExtendedModelMap) new ExtendedModelMap()).asMap()).isEmpty();
    verify(companyAdminService).findCompanies(searchForm);
  }

  @Test
  void detailShowsCompanyDetail() {
    CompanyDetail companyDetail = companyDetail(1L);
    when(companyAdminService.findCompanyDetail(1L)).thenReturn(companyDetail);

    String view = controller.detail(1L, new ExtendedModelMap());

    assertThat(view).isEqualTo("admin/company/company-detail");
    verify(companyAdminService).findCompanyDetail(1L);
  }

  @Test
  void newFormShowsCompanyForm() {
    String view = controller.newForm(new ExtendedModelMap());

    assertThat(view).isEqualTo("admin/company/company-form");
    assertThat((ExtendedModelMap) new ExtendedModelMap()).isNotNull();
  }

  @Test
  void editFormPreparesModel() {
    CompanyEditForm companyEditForm = new CompanyEditForm("北浜精密機器");
    when(companyAdminService.findCompanyEditForm(1L)).thenReturn(companyEditForm);
    when(companyAdminService.findCompanyDetail(1L)).thenReturn(companyDetail(1L));

    String view = controller.editForm(1L, new ExtendedModelMap());

    assertThat(view).isEqualTo("admin/company/company-edit");
    verify(companyAdminService).findCompanyEditForm(1L);
    verify(companyAdminService).findCompanyDetail(1L);
  }

  @Test
  void createShowsFormWhenValidationErrors() {
    CompanyForm form = companyForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyForm");
    bindingResult.rejectValue("name", "required", "入力必須です");

    String view = controller.create(form, bindingResult, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/company/company-form");
    verify(companyAdminService, never()).create(form);
  }

  @Test
  void createRejectsDuplicateCompanyCode() {
    CompanyForm form = companyForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyForm");
    when(companyAdminService.create(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社コードは既に使用されています。"));

    String view = controller.create(form, bindingResult, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/company/company-form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
    assertThat(bindingResult.getFieldError("code").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("code").getDefaultMessage())
        .isEqualTo("会社コードは既に使用されています。");
  }

  @Test
  void createRejectsDuplicateInitialTenantManagerUsername() {
    CompanyForm form = companyForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyForm");
    when(companyAdminService.create(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ユーザー名は既に使用されています。"));

    String view = controller.create(form, bindingResult, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/company/company-form");
    assertThat(bindingResult.getFieldError("initialTenantManagerUsername")).isNotNull();
    assertThat(bindingResult.getFieldError("initialTenantManagerUsername").getCode())
        .isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("initialTenantManagerUsername").getDefaultMessage())
        .isEqualTo("ユーザー名は既に使用されています。");
  }

  @Test
  void createRejectsDuplicateInitialTenantManagerEmail() {
    CompanyForm form = companyForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyForm");
    when(companyAdminService.create(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "emailは既に使用されています。"));

    String view = controller.create(form, bindingResult, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/company/company-form");
    assertThat(bindingResult.getFieldError("initialTenantManagerEmail")).isNotNull();
    assertThat(bindingResult.getFieldError("initialTenantManagerEmail").getCode())
        .isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("initialTenantManagerEmail").getDefaultMessage())
        .isEqualTo("emailは既に使用されています。");
  }

  @Test
  void createRejectsUnknownReason() {
    CompanyForm form = companyForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyForm");
    when(companyAdminService.create(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社初期化に失敗しました。"));

    String view = controller.create(form, bindingResult, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/company/company-form");
    assertThat(bindingResult.getGlobalError()).isNotNull();
    assertThat(bindingResult.getGlobalError().getCode()).isEqualTo("invalid");
  }

  @Test
  void createRethrowsInternalError() {
    CompanyForm form = companyForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyForm");
    when(companyAdminService.create(form))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "fatal"));

    assertThatThrownBy(
            () -> controller.create(form, bindingResult, new RedirectAttributesModelMap()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void createRedirectsAfterSuccess() {
    CompanyForm form = companyForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyForm");
    when(companyAdminService.create(form)).thenReturn(20L);
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.create(form, bindingResult, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/companies/20/departments");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "会社を登録しました。");
  }

  @Test
  void updateShowsFormWhenValidationErrors() {
    CompanyEditForm form = new CompanyEditForm("テスト");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyEditForm");
    bindingResult.rejectValue("name", "required", "入力必須です");
    when(companyAdminService.findCompanyEditForm(1L)).thenReturn(form);
    when(companyAdminService.findCompanyDetail(1L)).thenReturn(companyDetail(1L));

    String view =
        controller.update(
            1L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/company/company-edit");
    verify(companyAdminService, never()).updateCompany(1L, form);
  }

  @Test
  void updateRedirectsAfterSuccess() {
    CompanyEditForm form = new CompanyEditForm("テスト");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "companyEditForm");
    when(companyAdminService.findCompanyEditForm(1L)).thenReturn(form);
    when(companyAdminService.findCompanyDetail(1L)).thenReturn(companyDetail(1L));
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.update(1L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/companies/{companyId}");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "会社を更新しました。");
  }

  @Test
  void deleteRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.delete(1L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/companies?showDeleted=true");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "会社を削除しました。");
  }

  private CompanyForm companyForm() {
    return new CompanyForm(
        "NEW_COMPANY", "新会社", "initial-manager", "初期管理者", "initial-manager@example.local");
  }

  private CompanyListItem companyListItem() {
    return new CompanyListItem(
        1L, "KTHM", "北浜精密機器株式会社", false, 3L, 10L, 4L, 1L, LocalDateTime.of(2026, 1, 1, 9, 0));
  }

  private CompanyDetail companyDetail(Long id) {
    return new CompanyDetail(
        id,
        "KTHM",
        "北浜精密機器株式会社",
        false,
        3L,
        10L,
        4L,
        1L,
        2L,
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 10, 0));
  }
}
