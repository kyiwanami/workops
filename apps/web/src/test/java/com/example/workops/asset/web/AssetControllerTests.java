package com.example.workops.asset.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.asset.form.AssetForm;
import com.example.workops.asset.form.AssetSearchForm;
import com.example.workops.asset.form.AssetStatusForm;
import com.example.workops.asset.model.AssetCategoryOption;
import com.example.workops.asset.model.AssetDepartmentOption;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.asset.model.AssetListItem;
import com.example.workops.asset.model.AssetStatusOption;
import com.example.workops.asset.service.AssetCommandService;
import com.example.workops.asset.service.AssetQueryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class AssetControllerTests {

  private final AssetQueryService assetQueryService = mock(AssetQueryService.class);
  private final AssetCommandService assetCommandService = mock(AssetCommandService.class);
  private final AssetController controller =
      new AssetController(assetQueryService, assetCommandService);

  @Test
  void listLoadsAssetsAndOptions() {
    when(assetQueryService.findList(new AssetSearchForm("AST", "ノートPC", 1L, 2L, "ACTIVE")))
        .thenReturn(List.of(assetListItem()));
    when(assetQueryService.canCreateAsset()).thenReturn(true);
    when(assetQueryService.findAssetCategoryOptions()).thenReturn(List.of(assetCategoryOption()));
    when(assetQueryService.findDepartmentOptions()).thenReturn(List.of(assetDepartmentOption()));
    when(assetQueryService.findStatusOptions())
        .thenReturn(List.of(assetStatusOption("ACTIVE", "有効")));

    String view =
        controller.list(
            new AssetSearchForm("AST", "ノートPC", 1L, 2L, "ACTIVE"), new ExtendedModelMap());

    assertThat(view).isEqualTo("asset/list");
    assertThat((ExtendedModelMap) new ExtendedModelMap()).isNotNull();
    verify(assetQueryService).findList(new AssetSearchForm("AST", "ノートPC", 1L, 2L, "ACTIVE"));
  }

  @Test
  void newFormPreparesEmptyFormAndModel() {
    when(assetQueryService.findAssetCategoryOptions()).thenReturn(List.of(assetCategoryOption()));
    when(assetQueryService.findDepartmentOptions()).thenReturn(List.of(assetDepartmentOption()));
    when(assetQueryService.findStatusOptions())
        .thenReturn(List.of(assetStatusOption("ACTIVE", "有効")));
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.newForm(model);

    assertThat(view).isEqualTo("asset/form");
    assertThat(model.asMap())
        .containsEntry("assetForm", AssetForm.empty())
        .containsEntry("edit", false)
        .containsEntry("assetId", null);
  }

  @Test
  void createShowsFormWhenValidationErrors() {
    AssetForm form = assetForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetForm");
    bindingResult.rejectValue("name", "required", "入力必須です");
    when(assetQueryService.findAssetCategoryOptions()).thenReturn(List.of(assetCategoryOption()));
    when(assetQueryService.findDepartmentOptions()).thenReturn(List.of(assetDepartmentOption()));
    when(assetQueryService.findStatusOptions())
        .thenReturn(List.of(assetStatusOption("ACTIVE", "有効")));

    String view =
        controller.create(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("asset/form");
    assertThat(bindingResult.hasErrors()).isTrue();
    verify(assetQueryService).findAssetCategoryOptions();
  }

  @Test
  void createRejectsDuplicateCode() {
    AssetForm form = assetForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetForm");
    when(assetCommandService.isDuplicateCodeForCreate(form.code())).thenReturn(true);

    String view =
        controller.create(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("asset/form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
    assertThat(bindingResult.getFieldError("code").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("code").getDefaultMessage())
        .isEqualTo("資産コードは既に使用されています。");
  }

  @Test
  void createRedirectsAfterSuccess() {
    AssetForm form = assetForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetForm");
    when(assetCommandService.createAsset(form)).thenReturn(10L);

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    String view =
        controller.create(form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/assets/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産を登録しました。");
  }

  @Test
  void createRethrowsInternalServerError() {
    AssetForm form = assetForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetForm");
    when(assetCommandService.createAsset(form))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "fatal"));

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(
            () ->
                controller.create(
                    form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap()));
  }

  @Test
  void detailShowsAssetAndFlags() {
    AssetDetail detail = assetDetail(10L);
    when(assetQueryService.findDetail(10L)).thenReturn(detail);
    when(assetQueryService.canEditAsset(detail)).thenReturn(true);
    when(assetQueryService.canChangeStatus(detail)).thenReturn(true);
    when(assetQueryService.canDeleteAsset(detail)).thenReturn(false);

    String view = controller.detail(10L, new ExtendedModelMap());

    assertThat(view).isEqualTo("asset/detail");
  }

  @Test
  void editFormPreparesAssetForUpdate() {
    when(assetCommandService.findAssetForEdit(10L)).thenReturn(assetDetail(10L));
    when(assetQueryService.findAssetCategoryOptions()).thenReturn(List.of(assetCategoryOption()));
    when(assetQueryService.findDepartmentOptions()).thenReturn(List.of(assetDepartmentOption()));
    when(assetQueryService.findStatusOptions())
        .thenReturn(List.of(assetStatusOption("ACTIVE", "有効")));

    String view = controller.editForm(10L, new ExtendedModelMap());

    assertThat(view).isEqualTo("asset/form");
    verify(assetCommandService).findAssetForEdit(10L);
  }

  @Test
  void updateShowsFormWhenValidationErrors() {
    AssetDetail detail = assetDetail(10L);
    AssetForm form = assetForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetForm");
    bindingResult.rejectValue("code", "required", "入力必須です");
    when(assetCommandService.findAssetForEdit(10L)).thenReturn(detail);
    when(assetQueryService.findAssetCategoryOptions()).thenReturn(List.of(assetCategoryOption()));
    when(assetQueryService.findDepartmentOptions()).thenReturn(List.of(assetDepartmentOption()));
    when(assetQueryService.findStatusOptions())
        .thenReturn(List.of(assetStatusOption("ACTIVE", "有効")));

    String view =
        controller.update(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("asset/form");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void updateRejectsDuplicateCode() {
    AssetDetail detail = assetDetail(10L);
    AssetForm form = assetForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetForm");
    when(assetCommandService.findAssetForEdit(10L)).thenReturn(detail);
    when(assetCommandService.isDuplicateCodeForUpdate(10L, form.code())).thenReturn(true);

    String view =
        controller.update(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("asset/form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
    assertThat(bindingResult.getFieldError("code").getCode()).isEqualTo("duplicate");
  }

  @Test
  void updateRedirectsAfterSuccess() {
    AssetDetail detail = assetDetail(10L);
    AssetForm form = assetForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetForm");
    when(assetCommandService.findAssetForEdit(10L)).thenReturn(detail);

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    String view =
        controller.update(10L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/assets/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産を更新しました。");
  }

  @Test
  void statusFormPreparesStatusFormModel() {
    AssetDetail detail = assetDetail(10L);
    when(assetCommandService.findAssetForStatusChange(10L)).thenReturn(detail);
    when(assetQueryService.findStatusOptions())
        .thenReturn(
            List.of(assetStatusOption("ACTIVE", "有効"), assetStatusOption("INACTIVE", "無効")));

    String view = controller.statusForm(10L, new ExtendedModelMap());

    assertThat(view).isEqualTo("asset/status-form");
    verify(assetCommandService).findAssetForStatusChange(10L);
  }

  @Test
  void updateStatusShowsFormWhenValidationErrors() {
    AssetDetail detail = assetDetail(10L);
    AssetStatusForm form = new AssetStatusForm("");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetStatusForm");
    when(assetQueryService.findStatusOptions())
        .thenReturn(List.of(assetStatusOption("ACTIVE", "有効")));
    when(assetCommandService.findAssetForStatusChange(10L)).thenReturn(detail);
    bindingResult.addError(new ObjectError("assetStatusForm", "入力必須です"));

    String view =
        controller.updateStatus(
            10L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("asset/status-form");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void updateStatusRedirectsAfterSuccess() {
    AssetDetail detail = assetDetail(10L);
    AssetStatusForm form = new AssetStatusForm("INACTIVE");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetStatusForm");
    when(assetCommandService.findAssetForStatusChange(10L)).thenReturn(detail);
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.updateStatus(
            10L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/assets/10");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産ステータスを変更しました。");
  }

  @Test
  void deleteRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.delete(10L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/assets");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産を削除しました。");
  }

  private AssetForm assetForm() {
    return new AssetForm("AST-001", "ノートPC", 100L, 200L, "ACTIVE", "備考");
  }

  private AssetDetail assetDetail(Long id) {
    return new AssetDetail(
        id,
        1L,
        2L,
        "AST-001",
        "ノートPC",
        "AST-CAT-1",
        "資産分類",
        false,
        "営業部",
        "ACTIVE",
        "有効",
        "備考",
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }

  private AssetListItem assetListItem() {
    return new AssetListItem(
        10L,
        "AST-001",
        "ノートPC",
        "AST-CAT-1",
        "資産分類",
        false,
        "営業部",
        "ACTIVE",
        "有効",
        "備考",
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }

  private AssetCategoryOption assetCategoryOption() {
    return new AssetCategoryOption(1L, "AST-CAT-1", "資産分類");
  }

  private AssetDepartmentOption assetDepartmentOption() {
    return new AssetDepartmentOption(2L, "DEP-1", "営業部");
  }

  private AssetStatusOption assetStatusOption(String code, String name) {
    return new AssetStatusOption(code, name);
  }
}
