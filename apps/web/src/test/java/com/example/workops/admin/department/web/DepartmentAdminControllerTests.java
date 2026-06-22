package com.example.workops.admin.department.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.admin.department.form.DepartmentForm;
import com.example.workops.admin.department.form.DepartmentSearchForm;
import com.example.workops.admin.department.model.DepartmentListItem;
import com.example.workops.admin.department.model.DepartmentListPage;
import com.example.workops.admin.department.service.DepartmentAdminService;
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

class DepartmentAdminControllerTests {

  private final DepartmentAdminService departmentAdminService = mock(DepartmentAdminService.class);
  private final DepartmentAdminController controller =
      new DepartmentAdminController(departmentAdminService);

  @Test
  void platformListShowsDepartmentList() {
    DepartmentListPage page = departmentListPage(1L, true);
    when(departmentAdminService.findPlatformDepartmentList(1L, new DepartmentSearchForm(false)))
        .thenReturn(page);
    DepartmentSearchForm searchForm = new DepartmentSearchForm(false);
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.platformList(1L, searchForm, model);

    assertThat(view).isEqualTo("admin/department/department-list");
    assertThat(model.asMap())
        .containsEntry("departmentListPage", page)
        .containsEntry("platformAdmin", true);
    verify(departmentAdminService).findPlatformDepartmentList(1L, searchForm);
  }

  @Test
  void platformNewFormLoadsFormModel() {
    DepartmentListPage page = departmentListPage(1L, false);
    when(departmentAdminService.findPlatformDepartmentFormPage(1L)).thenReturn(page);
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.platformNewForm(1L, model);

    assertThat(view).isEqualTo("admin/department/department-form");
    assertThat(model.asMap())
        .containsEntry("departmentListPage", page)
        .containsEntry("platformAdmin", true)
        .containsEntry("edit", false)
        .containsEntry("departmentId", null)
        .containsEntry("formAction", "/admin/companies/1/departments")
        .containsEntry("cancelHref", "/admin/companies/1/departments");
    assertThat(model.asMap()).containsEntry("departmentForm", DepartmentForm.empty());
  }

  @Test
  void platformCreateShowsFormWhenValidationErrors() {
    DepartmentListPage page = departmentListPage(1L, false);
    when(departmentAdminService.findPlatformDepartmentFormPage(1L)).thenReturn(page);
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    bindingResult.rejectValue("code", "required", "入力必須です");

    String view =
        controller.platformCreate(
            1L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    assertThat(bindingResult.hasErrors()).isTrue();
    verify(departmentAdminService).findPlatformDepartmentFormPage(1L);
    verify(departmentAdminService, org.mockito.Mockito.never()).createPlatformDepartment(1L, form);
  }

  @Test
  void platformCreateRejectsDuplicateCode() {
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    when(departmentAdminService.findPlatformDepartmentFormPage(1L))
        .thenReturn(departmentListPage(1L, false));
    when(departmentAdminService.createPlatformDepartment(1L, form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "部署コードは既に使用されています。"));

    String view =
        controller.platformCreate(
            1L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
    assertThat(bindingResult.getFieldError("code").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("code").getDefaultMessage())
        .isEqualTo("部署コードは既に使用されています。");
  }

  @Test
  void platformCreateRethrowsInternalServerError() {
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    when(departmentAdminService.findPlatformDepartmentFormPage(1L))
        .thenReturn(departmentListPage(1L, false));
    when(departmentAdminService.createPlatformDepartment(1L, form))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "fatal"));

    assertThatThrownBy(
            () ->
                controller.platformCreate(
                    1L,
                    form,
                    bindingResult,
                    new ExtendedModelMap(),
                    new RedirectAttributesModelMap()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void platformCreateRedirectsAfterSuccess() {
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(departmentAdminService.createPlatformDepartment(1L, form)).thenReturn(10L);

    String view =
        controller.platformCreate(
            1L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/companies/{companyId}/departments");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "部署を登録しました。");
  }

  @Test
  void platformEditFormLoadsFormAndModel() {
    when(departmentAdminService.findPlatformDepartmentFormPage(1L))
        .thenReturn(departmentListPage(1L, false));
    when(departmentAdminService.findPlatformDepartmentForEdit(1L, 3L))
        .thenReturn(departmentListItem(3L));

    String view = controller.platformEditForm(1L, 3L, new ExtendedModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
  }

  @Test
  void platformUpdateShowsFormWhenHasErrors() {
    DepartmentListPage page = departmentListPage(1L, false);
    when(departmentAdminService.findPlatformDepartmentFormPage(1L)).thenReturn(page);
    when(departmentAdminService.findPlatformDepartmentForEdit(1L, 3L))
        .thenReturn(departmentListItem(3L));
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    bindingResult.rejectValue("name", "required", "入力必須です");

    String view =
        controller.platformUpdate(
            1L, 3L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    assertThat(bindingResult.hasErrors()).isTrue();
    verify(departmentAdminService).findPlatformDepartmentForEdit(1L, 3L);
    verify(departmentAdminService).findPlatformDepartmentFormPage(1L);
    verify(departmentAdminService, org.mockito.Mockito.never())
        .updatePlatformDepartment(1L, 3L, form);
  }

  @Test
  void platformUpdateRedirectsAfterSuccess() {
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    when(departmentAdminService.findPlatformDepartmentFormPage(1L))
        .thenReturn(departmentListPage(1L, false));
    when(departmentAdminService.findPlatformDepartmentForEdit(1L, 3L))
        .thenReturn(departmentListItem(3L));
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.platformUpdate(
            1L, 3L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/companies/{companyId}/departments");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "部署を更新しました。");
  }

  @Test
  void platformDeleteRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.platformDelete(1L, 3L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/companies/{companyId}/departments");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "部署を削除しました。");
  }

  @Test
  void tenantListShowsDepartmentList() {
    DepartmentListPage page = departmentListPage(2L, true);
    when(departmentAdminService.findTenantDepartmentList(new DepartmentSearchForm(false)))
        .thenReturn(page);
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.tenantList(new DepartmentSearchForm(false), model);

    assertThat(view).isEqualTo("admin/department/department-list");
    assertThat(model.asMap())
        .containsEntry("departmentListPage", page)
        .containsEntry("platformAdmin", false);
    verify(departmentAdminService).findTenantDepartmentList(new DepartmentSearchForm(false));
  }

  @Test
  void tenantNewFormLoadsFormAndModel() {
    when(departmentAdminService.findTenantDepartmentFormPage())
        .thenReturn(departmentListPage(2L, false));

    String view = controller.tenantNewForm(new ExtendedModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    verify(departmentAdminService).findTenantDepartmentFormPage();
  }

  @Test
  void tenantCreateShowsFormWhenValidationErrors() {
    DepartmentListPage page = departmentListPage(2L, false);
    when(departmentAdminService.findTenantDepartmentFormPage()).thenReturn(page);
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    bindingResult.rejectValue("code", "required", "入力必須です");

    String view =
        controller.tenantCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    verify(departmentAdminService).findTenantDepartmentFormPage();
    verify(departmentAdminService, org.mockito.Mockito.never()).createTenantDepartment(form);
  }

  @Test
  void tenantCreateRejectsDuplicateCode() {
    DepartmentListPage page = departmentListPage(2L, false);
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    when(departmentAdminService.findTenantDepartmentFormPage()).thenReturn(page);
    when(departmentAdminService.createTenantDepartment(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "部署コードは既に使用されています。"));

    String view =
        controller.tenantCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
    assertThat(bindingResult.getFieldError("code").getCode()).isEqualTo("duplicate");
  }

  @Test
  void tenantCreateRethrowsInternalServerError() {
    DepartmentListPage page = departmentListPage(2L, false);
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    when(departmentAdminService.findTenantDepartmentFormPage()).thenReturn(page);
    when(departmentAdminService.createTenantDepartment(form))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "fatal"));

    assertThatThrownBy(
            () ->
                controller.tenantCreate(
                    form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void tenantCreateRedirectsAfterSuccess() {
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(departmentAdminService.createTenantDepartment(form)).thenReturn(20L);

    String view =
        controller.tenantCreate(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/departments");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "部署を登録しました。");
  }

  @Test
  void tenantEditFormLoadsFormAndModel() {
    when(departmentAdminService.findTenantDepartmentFormPage())
        .thenReturn(departmentListPage(2L, false));
    when(departmentAdminService.findTenantDepartmentForEdit(3L)).thenReturn(departmentListItem(3L));

    String view = controller.tenantEditForm(3L, new ExtendedModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    verify(departmentAdminService).findTenantDepartmentFormPage();
    verify(departmentAdminService).findTenantDepartmentForEdit(3L);
  }

  @Test
  void tenantUpdateShowsFormWhenHasErrors() {
    when(departmentAdminService.findTenantDepartmentFormPage())
        .thenReturn(departmentListPage(2L, false));
    when(departmentAdminService.findTenantDepartmentForEdit(3L)).thenReturn(departmentListItem(3L));
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    bindingResult.rejectValue("name", "required", "入力必須です");

    String view =
        controller.tenantUpdate(
            3L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/department/department-form");
    verify(departmentAdminService, org.mockito.Mockito.never()).updateTenantDepartment(3L, form);
  }

  @Test
  void tenantUpdateRedirectsAfterSuccess() {
    DepartmentForm form = new DepartmentForm("DEV", "開発部");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "departmentForm");
    when(departmentAdminService.findTenantDepartmentFormPage())
        .thenReturn(departmentListPage(2L, false));
    when(departmentAdminService.findTenantDepartmentForEdit(3L)).thenReturn(departmentListItem(3L));
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.tenantUpdate(
            3L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/departments");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "部署を更新しました。");
  }

  @Test
  void tenantDeleteRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.tenantDelete(4L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/departments");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "部署を削除しました。");
  }

  private DepartmentListPage departmentListPage(Long companyId, boolean includeDepartments) {
    return new DepartmentListPage(
        companyId,
        "KTHM",
        "北浜精密機器株式会社",
        false,
        includeDepartments
            ? List.of(departmentListItem(10L), departmentListItem(11L))
            : List.<DepartmentListItem>of());
  }

  private DepartmentListItem departmentListItem(Long id) {
    return new DepartmentListItem(
        id,
        "DEV" + id,
        "開発部",
        false,
        4L,
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }
}
