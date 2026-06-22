package com.example.workops.admin.user.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.admin.user.form.UserEditForm;
import com.example.workops.admin.user.form.UserForm;
import com.example.workops.admin.user.model.CompanySelectOption;
import com.example.workops.admin.user.model.DepartmentSelectOption;
import com.example.workops.admin.user.model.PermissionSetOption;
import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserEditTarget;
import com.example.workops.admin.user.model.UserListItem;
import com.example.workops.admin.user.service.UserAdminService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class UserAdminControllerTests {

  private final UserAdminService userAdminService = mock(UserAdminService.class);
  private final UserAdminController controller = new UserAdminController(userAdminService);

  @Test
  void platformListAddsPlatformUsersAndMode() {
    List<UserListItem> users = List.of(userListItem(1L, "platform-admin", "PLATFORM", null));
    when(userAdminService.findPlatformUsers()).thenReturn(users);
    Model model = new ExtendedModelMap();

    String view = controller.platformList(model);

    assertThat(view).isEqualTo("admin/user/user-list");
    assertThat(model.asMap()).containsEntry("users", users).containsEntry("platformAdmin", true);
    verify(userAdminService).findPlatformUsers();
  }

  @Test
  void platformNewFormPreparesOptions() {
    stubPlatformFormOptions();
    Model model = new ExtendedModelMap();

    String view = controller.platformNewForm(model);

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(model.asMap())
        .containsEntry("userForm", UserForm.empty())
        .containsEntry("formMode", "platform")
        .containsEntry("formAction", "/admin/users")
        .containsEntry("cancelHref", "/admin/users")
        .containsEntry("companies", List.of(companyOption()))
        .containsEntry("departments", List.of(departmentOption()))
        .containsEntry("platformPermissionSets", List.of(platformPermissionSetOption()))
        .containsEntry("tenantPermissionSets", List.of(tenantPermissionSetOption()));
  }

  @Test
  void platformCreateShowsPlatformFormWhenFormHasErrors() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    bindingResult.rejectValue("name", "required", "入力必須です");
    stubPlatformFormOptions();
    Model model = new ExtendedModelMap();

    String view =
        controller.platformCreate(form, bindingResult, model, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(model.asMap())
        .containsEntry("formMode", "platform")
        .containsEntry("formAction", "/admin/users")
        .containsEntry("cancelHref", "/admin/users");
    verify(userAdminService).findActiveCompanies();
    verify(userAdminService).findActiveDepartments();
    verify(userAdminService).findPlatformPermissionSetOptions();
    verify(userAdminService).findTenantPermissionSetOptions();
    verify(userAdminService, never()).createPlatformUser(form);
  }

  @Test
  void platformCreateRejectsDuplicateUsername() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createPlatformUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ユーザー名は既に使用されています。"));

    String view =
        controller.platformCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(bindingResult.getFieldError("username")).isNotNull();
    assertThat(bindingResult.getFieldError("username").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("username").getDefaultMessage())
        .isEqualTo("ユーザー名は既に使用されています。");
  }

  @Test
  void platformCreateRejectsUnknownBadRequestInGeneral() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createPlatformUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "予期しないユーザー作成エラー"));

    String view =
        controller.platformCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(bindingResult.getGlobalError()).isNotNull();
    assertThat(bindingResult.getGlobalError().getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getGlobalError().getDefaultMessage()).isEqualTo("予期しないユーザー作成エラー");
  }

  @Test
  void platformCreateRejectsCompanyNotSelected() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createPlatformUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社を選択してください。"));

    String view =
        controller.platformCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(bindingResult.getFieldError("companyId")).isNotNull();
    assertThat(bindingResult.getFieldError("companyId").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("companyId").getDefaultMessage())
        .isEqualTo("会社を選択してください。");
  }

  @Test
  void platformCreateRejectsCompanyNotFound() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createPlatformUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社が見つかりません。"));

    String view =
        controller.platformCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(bindingResult.getFieldError("companyId")).isNotNull();
    assertThat(bindingResult.getFieldError("companyId").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("companyId").getDefaultMessage())
        .isEqualTo("会社が見つかりません。");
  }

  @Test
  void platformCreateRejectsDepartmentNotFound() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createPlatformUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "所属部署が見つかりません。"));

    String view =
        controller.platformCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(bindingResult.getFieldError("departmentId")).isNotNull();
    assertThat(bindingResult.getFieldError("departmentId").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("departmentId").getDefaultMessage())
        .isEqualTo("所属部署が見つかりません。");
  }

  @Test
  void platformCreateRejectsInvalidActorType() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createPlatformUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "actor_typeが不正です。"));

    String view =
        controller.platformCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(bindingResult.getFieldError("actorType")).isNotNull();
    assertThat(bindingResult.getFieldError("actorType").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("actorType").getDefaultMessage())
        .isEqualTo("actor_typeが不正です。");
  }

  @Test
  void platformCreateRethrowsInternalServerError() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createPlatformUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "fatal"));

    assertThatThrownBy(
            () ->
                controller.platformCreate(
                    form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void platformCreateRedirectsAfterSuccess() {
    UserForm form = userFormPlatform();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(userAdminService.createPlatformUser(form)).thenReturn(100L);

    String view =
        controller.platformCreate(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users/100");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "ユーザーを登録しました。");
  }

  @Test
  void platformDetailAddsModeAndNavigation() {
    UserDetail detail = userDetail(10L, "platform-admin", "PLATFORM", null);
    when(userAdminService.findPlatformUserDetail(10L)).thenReturn(detail);
    Model model = new ExtendedModelMap();

    String view = controller.platformDetail(10L, model);

    assertThat(view).isEqualTo("admin/user/user-detail");
    assertThat(model.asMap())
        .containsEntry("user", detail)
        .containsEntry("platformAdmin", true)
        .containsEntry("listHref", "/admin/users")
        .containsEntry("editHref", "/admin/users/10/edit");
  }

  @Test
  void platformEditFormPreparesPlatformFormModel() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form =
        new UserEditForm("表示名", "platform-admin@example.local", 1L, List.of("PLATFORM_ADMIN"));
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findPlatformUserEditForm(20L)).thenReturn(form);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.of(departmentOption()));
    when(userAdminService.findPlatformPermissionSetOptions())
        .thenReturn(List.of(platformPermissionSetOption()));

    Model model = new ExtendedModelMap();

    String view = controller.platformEditForm(20L, model);

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(model.asMap())
        .containsEntry("user", target)
        .containsEntry("formAction", "/admin/users/20/edit")
        .containsEntry("cancelHref", "/admin/users/20")
        .containsEntry("departments", List.of(departmentOption()))
        .containsEntry("permissionSets", List.of(platformPermissionSetOption()));
    verify(userAdminService).findPlatformUserEditTarget(20L);
    verify(userAdminService).findPlatformUserEditForm(20L);
    verify(userAdminService).findActiveDepartmentsByCompanyId(10L);
    verify(userAdminService).findPlatformPermissionSetOptions();
  }

  @Test
  void platformUpdateShowsFormWhenHasErrors() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", null);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    bindingResult.rejectValue("name", "required", "入力必須です");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.of(departmentOption()));

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    verify(userAdminService).findPlatformUserEditTarget(20L);
    verify(userAdminService, never()).updatePlatformUser(20L, form);
  }

  @Test
  void platformUpdateRejectsDepartmentNotFound() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", null);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.<DepartmentSelectOption>of());
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "所属部署が見つかりません。"))
        .when(userAdminService)
        .updatePlatformUser(20L, form);

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getFieldError("departmentId")).isNotNull();
    assertThat(bindingResult.getFieldError("departmentId").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("departmentId").getDefaultMessage())
        .isEqualTo("所属部署が見つかりません。");
  }

  @Test
  void platformUpdateRejectsTenantDepartmentOnPlatformUser() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", 1L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.of(departmentOption()));
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORMユーザーには部署を指定できません。"))
        .when(userAdminService)
        .updatePlatformUser(20L, form);

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getFieldError("departmentId")).isNotNull();
    assertThat(bindingResult.getFieldError("departmentId").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("departmentId").getDefaultMessage())
        .isEqualTo("PLATFORMユーザーには部署を指定できません。");
  }

  @Test
  void platformUpdateRejectsInvalidPermissionSetForPlatformUser() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", null);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.of(departmentOption()));
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORMユーザーの権限セットが不正です。"))
        .when(userAdminService)
        .updatePlatformUser(20L, form);

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getFieldError("permissionSetCodes")).isNotNull();
    assertThat(bindingResult.getFieldError("permissionSetCodes").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("permissionSetCodes").getDefaultMessage())
        .isEqualTo("PLATFORMユーザーの権限セットが不正です。");
  }

  @Test
  void platformUpdateRejectsEmptyPermissionSetForPlatformUser() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", null);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.of(departmentOption()));
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "権限セットを選択してください。"))
        .when(userAdminService)
        .updatePlatformUser(20L, form);

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getFieldError("permissionSetCodes")).isNotNull();
    assertThat(bindingResult.getFieldError("permissionSetCodes").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("permissionSetCodes").getDefaultMessage())
        .isEqualTo("権限セットを選択してください。");
  }

  @Test
  void tenantUpdateRejectsInvalidPermissionSetForTenantUser() {
    UserEditTarget target = userEditTarget(10L, 3L, "TENANT");
    UserEditForm form = userEditForm("表示名", "tenant-user@example.local", 3L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findTenantUserEditTarget(10L)).thenReturn(target);
    when(userAdminService.findTenantActiveDepartments()).thenReturn(List.of(departmentOption()));
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "TENANTユーザーの権限セットが不正です。"))
        .when(userAdminService)
        .updateTenantUser(10L, form);

    String view =
        controller.tenantUpdate(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getFieldError("permissionSetCodes")).isNotNull();
    assertThat(bindingResult.getFieldError("permissionSetCodes").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("permissionSetCodes").getDefaultMessage())
        .isEqualTo("TENANTユーザーの権限セットが不正です。");
  }

  @Test
  void platformUpdateRethrowsInternalServerError() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", null);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.<DepartmentSelectOption>of());
    doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "fatal"))
        .when(userAdminService)
        .updatePlatformUser(20L, form);

    assertThatThrownBy(
            () ->
                controller.platformUpdate(
                    20L,
                    form,
                    bindingResult,
                    new ExtendedModelMap(),
                    new RedirectAttributesModelMap()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void platformUpdateRedirectsAfterSuccess() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", 1L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findActiveDepartmentsByCompanyId(10L))
        .thenReturn(List.of(departmentOption()));

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/admin/users/20");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "ユーザーを更新しました。");
    verify(userAdminService).updatePlatformUser(20L, form);
  }

  @Test
  void tenantListAddsTenantUsersAndMode() {
    List<UserListItem> users = List.of(userListItem(3L, "tenant-user", "TENANT", "Sample"));
    when(userAdminService.findTenantUsers()).thenReturn(users);
    Model model = new ExtendedModelMap();

    String view = controller.tenantList(model);

    assertThat(view).isEqualTo("admin/user/user-list");
    assertThat(model.asMap()).containsEntry("users", users).containsEntry("platformAdmin", false);
    verify(userAdminService).findTenantUsers();
  }

  @Test
  void tenantNewFormPreparesTenantOptions() {
    when(userAdminService.findTenantActiveDepartments()).thenReturn(List.of(departmentOption()));
    when(userAdminService.findTenantPermissionSetOptions())
        .thenReturn(List.of(tenantPermissionSetOption()));
    Model model = new ExtendedModelMap();

    String view = controller.tenantNewForm(model);

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(model.asMap())
        .containsEntry("userForm", UserForm.empty())
        .containsEntry("formMode", "tenant")
        .containsEntry("formAction", "/users")
        .containsEntry("cancelHref", "/users")
        .containsEntry("departments", List.of(departmentOption()))
        .containsEntry("tenantPermissionSets", List.of(tenantPermissionSetOption()));
  }

  @Test
  void tenantCreateShowsFormWhenFormHasErrors() {
    UserForm form = userFormTenant();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    bindingResult.rejectValue("name", "required", "入力必須です");
    when(userAdminService.findTenantActiveDepartments()).thenReturn(List.of(departmentOption()));
    when(userAdminService.findTenantPermissionSetOptions())
        .thenReturn(List.of(tenantPermissionSetOption()));
    Model model = new ExtendedModelMap();

    String view =
        controller.tenantCreate(form, bindingResult, model, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(model.asMap())
        .containsEntry("formMode", "tenant")
        .containsEntry("formAction", "/users")
        .containsEntry("cancelHref", "/users");
    verify(userAdminService).findTenantActiveDepartments();
    verify(userAdminService).findTenantPermissionSetOptions();
    verify(userAdminService, never()).createTenantUser(form);
  }

  @Test
  void tenantCreateRejectsDuplicateEmail() {
    UserForm form = userFormTenant();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createTenantUser(form))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "emailは既に使用されています。"));

    String view =
        controller.tenantCreate(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-form");
    assertThat(bindingResult.getFieldError("email")).isNotNull();
    assertThat(bindingResult.getFieldError("email").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("email").getDefaultMessage())
        .isEqualTo("emailは既に使用されています。");
  }

  @Test
  void tenantCreateRethrowsInternalServerError() {
    UserForm form = userFormTenant();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    when(userAdminService.createTenantUser(form))
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
    UserForm form = userFormTenant();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(userAdminService.createTenantUser(form)).thenReturn(50L);

    String view =
        controller.tenantCreate(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/users/50");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "ユーザーを登録しました。");
  }

  @Test
  void tenantDetailAddsModeAndNavigation() {
    UserDetail detail = userDetail(10L, "tenant-user", "TENANT", "Sample");
    when(userAdminService.findTenantUserDetail(10L)).thenReturn(detail);
    Model model = new ExtendedModelMap();

    String view = controller.tenantDetail(10L, model);

    assertThat(view).isEqualTo("admin/user/user-detail");
    assertThat(model.asMap())
        .containsEntry("user", detail)
        .containsEntry("platformAdmin", false)
        .containsEntry("listHref", "/users")
        .containsEntry("editHref", "/users/10/edit");
  }

  @Test
  void tenantEditFormPreparesTenantFormModel() {
    UserEditTarget target = userEditTarget(10L, 3L, "TENANT");
    UserEditForm form = userEditForm("表示名", "tenant-user@example.local", 3L);
    when(userAdminService.findTenantUserEditTarget(10L)).thenReturn(target);
    when(userAdminService.findTenantUserEditForm(10L)).thenReturn(form);
    when(userAdminService.findTenantActiveDepartments()).thenReturn(List.of(departmentOption()));
    when(userAdminService.findTenantPermissionSetOptions())
        .thenReturn(List.of(tenantPermissionSetOption()));

    Model model = new ExtendedModelMap();

    String view = controller.tenantEditForm(10L, model);

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(model.asMap())
        .containsEntry("user", target)
        .containsEntry("userEditForm", form)
        .containsEntry("formAction", "/users/10/edit")
        .containsEntry("cancelHref", "/users/10")
        .containsEntry("departments", List.of(departmentOption()))
        .containsEntry("permissionSets", List.of(tenantPermissionSetOption()));
  }

  @Test
  void tenantUpdateRejectsDuplicateManagerRequirement() {
    UserEditTarget target = userEditTarget(10L, 3L, "TENANT");
    UserEditForm form = userEditForm("表示名", "tenant-user@example.local", 3L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findTenantUserEditTarget(10L)).thenReturn(target);
    when(userAdminService.findTenantActiveDepartments()).thenReturn(List.of(departmentOption()));
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "TENANT_MANAGERは最低1人必要です。"))
        .when(userAdminService)
        .updateTenantUser(10L, form);

    String view =
        controller.tenantUpdate(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getFieldError("permissionSetCodes")).isNotNull();
    assertThat(bindingResult.getFieldError("permissionSetCodes").getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getFieldError("permissionSetCodes").getDefaultMessage())
        .isEqualTo("TENANT_MANAGERは最低1人必要です。");
  }

  @Test
  void tenantUpdateRedirectsAfterSuccess() {
    UserEditTarget target = userEditTarget(10L, 3L, "TENANT");
    UserEditForm form = userEditForm("表示名", "tenant-user@example.local", 3L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(userAdminService.findTenantUserEditTarget(10L)).thenReturn(target);
    when(userAdminService.findTenantActiveDepartments()).thenReturn(List.of(departmentOption()));

    String view =
        controller.tenantUpdate(
            10L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/users/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "ユーザーを更新しました。");
    verify(userAdminService).updateTenantUser(10L, form);
  }

  @Test
  void tenantUpdateShowsFormWhenValidationErrors() {
    UserEditTarget target = userEditTarget(10L, 3L, "TENANT");
    UserEditForm form = userEditForm("表示名", "tenant-user@example.local", 3L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    bindingResult.rejectValue("name", "required", "入力必須です");
    when(userAdminService.findTenantUserEditTarget(10L)).thenReturn(target);

    String view =
        controller.tenantUpdate(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.hasErrors()).isTrue();
    verify(userAdminService).findTenantUserEditTarget(10L);
    verify(userAdminService, never()).updateTenantUser(10L, form);
  }

  @Test
  void tenantUpdateRethrowsInternalServerError() {
    UserEditTarget target = userEditTarget(10L, 3L, "TENANT");
    UserEditForm form = userEditForm("表示名", "tenant-user@example.local", 3L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findTenantUserEditTarget(10L)).thenReturn(target);
    doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "fatal"))
        .when(userAdminService)
        .updateTenantUser(10L, form);

    assertThatThrownBy(
            () ->
                controller.tenantUpdate(
                    10L,
                    form,
                    bindingResult,
                    new ExtendedModelMap(),
                    new RedirectAttributesModelMap()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void platformEditFormUsesTenantPermissionSetsWhenUserIsTenant() {
    UserEditTarget target = userEditTarget(20L, 10L, "TENANT");
    UserEditForm form = userEditForm("表示名", "tenant-user@example.local", 3L);
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    when(userAdminService.findPlatformUserEditForm(20L)).thenReturn(form);
    when(userAdminService.findTenantPermissionSetOptions())
        .thenReturn(List.of(tenantPermissionSetOption()));

    ExtendedModelMap model = new ExtendedModelMap();
    String view = controller.platformEditForm(20L, model);

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(model.asMap())
        .containsEntry("user", target)
        .containsEntry("permissionSets", List.of(tenantPermissionSetOption()))
        .containsEntry("formAction", "/admin/users/20/edit")
        .containsEntry("cancelHref", "/admin/users/20");
    verify(userAdminService).findTenantPermissionSetOptions();
  }

  @Test
  void platformUpdateRejectsEmailDuplicate() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", 1L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "emailは既に使用されています。"))
        .when(userAdminService)
        .updatePlatformUser(20L, form);

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getFieldError("email")).isNotNull();
    assertThat(bindingResult.getFieldError("email").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("email").getDefaultMessage())
        .isEqualTo("emailは既に使用されています。");
  }

  @Test
  void platformUpdateRejectsUnknownBadRequestInGeneral() {
    UserEditTarget target = userEditTarget(20L, 10L, "PLATFORM");
    UserEditForm form = userEditForm("表示名", "platform-admin@example.local", 1L);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "userEditForm");
    when(userAdminService.findPlatformUserEditTarget(20L)).thenReturn(target);
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知のエラーが発生しました。"))
        .when(userAdminService)
        .updatePlatformUser(20L, form);

    String view =
        controller.platformUpdate(
            20L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("admin/user/user-edit");
    assertThat(bindingResult.getGlobalError()).isNotNull();
    assertThat(bindingResult.getGlobalError().getCode()).isEqualTo("invalid");
    assertThat(bindingResult.getGlobalError().getDefaultMessage()).isEqualTo("未知のエラーが発生しました。");
  }

  private void stubPlatformFormOptions() {
    when(userAdminService.findActiveCompanies()).thenReturn(List.of(companyOption()));
    when(userAdminService.findActiveDepartments()).thenReturn(List.of(departmentOption()));
    when(userAdminService.findPlatformPermissionSetOptions())
        .thenReturn(List.of(platformPermissionSetOption()));
    when(userAdminService.findTenantPermissionSetOptions())
        .thenReturn(List.of(tenantPermissionSetOption()));
  }

  private CompanySelectOption companyOption() {
    return new CompanySelectOption(10L, "KTHM", "北浜精密機器");
  }

  private DepartmentSelectOption departmentOption() {
    return new DepartmentSelectOption(1L, 10L, "KTHM", "営業部");
  }

  private PermissionSetOption platformPermissionSetOption() {
    return new PermissionSetOption("PLATFORM_ADMIN", "PLATFORM管理者");
  }

  private PermissionSetOption tenantPermissionSetOption() {
    return new PermissionSetOption("TENANT_MANAGER", "テナント管理者");
  }

  private UserForm userFormPlatform() {
    return new UserForm(
        "PLATFORM",
        null,
        null,
        "platform-admin",
        "Platform Admin",
        "platform-admin@example.local",
        List.of("PLATFORM_ADMIN"));
  }

  private UserForm userFormTenant() {
    return new UserForm(
        "TENANT",
        1L,
        2L,
        "tenant-user",
        "Tenant User",
        "tenant-user@example.local",
        List.of("TENANT_MANAGER"));
  }

  private UserEditForm userEditForm(String name, String email, Long departmentId) {
    return new UserEditForm(name, email, departmentId, List.of("TENANT_MANAGER"));
  }

  private UserListItem userListItem(
      Long id, String username, String actorType, String companyName) {
    return new UserListItem(
        id,
        username,
        "表示名",
        username + "@example.local",
        actorType,
        companyName,
        "総務部",
        "TENANT_MANAGER / 管理者",
        LocalDateTime.of(2026, 5, 1, 9, 0));
  }

  private UserDetail userDetail(Long id, String username, String actorType, String companyName) {
    return new UserDetail(
        id,
        actorType.equals("PLATFORM") ? null : 10L,
        username,
        "表示名",
        username + "@example.local",
        actorType,
        companyName,
        "総務部",
        "TENANT_MANAGER / 管理者",
        "cognito-sub",
        false,
        LocalDateTime.of(2026, 5, 1, 9, 0),
        LocalDateTime.of(2026, 5, 1, 9, 0));
  }

  private UserEditTarget userEditTarget(Long id, Long companyId, String actorType) {
    return new UserEditTarget(
        id,
        companyId,
        1L,
        "username",
        "表示名",
        "user@example.local",
        actorType,
        "会社名",
        "cognito-sub");
  }
}
