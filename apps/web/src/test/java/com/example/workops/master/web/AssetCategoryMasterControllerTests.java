package com.example.workops.master.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.workops.master.form.AssetCategoryMasterForm;
import com.example.workops.master.form.AssetCategoryMasterSearchForm;
import com.example.workops.master.model.AssetCategoryMasterDetail;
import com.example.workops.master.model.AssetCategoryMasterListItem;
import com.example.workops.master.service.AssetCategoryMasterService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class AssetCategoryMasterControllerTests {

  private final AssetCategoryMasterService assetCategoryMasterService =
      mock(AssetCategoryMasterService.class);
  private final AssetCategoryMasterController controller =
      new AssetCategoryMasterController(assetCategoryMasterService);

  @Test
  void listShowsAssetCategories() {
    AssetCategoryMasterSearchForm searchForm = new AssetCategoryMasterSearchForm(false);
    when(assetCategoryMasterService.findList(searchForm))
        .thenReturn(List.of(assetCategoryMasterListItem(1L, "CAT01")));
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.list(searchForm, model);

    assertThat(view).isEqualTo("master/asset-category-list");
    assertThat(model.asMap())
        .containsEntry("assetCategories", List.of(assetCategoryMasterListItem(1L, "CAT01")));
  }

  @Test
  void newFormShowsEmptyForm() {
    String view = controller.newForm(new ExtendedModelMap());

    assertThat(view).isEqualTo("master/asset-category-form");
  }

  @Test
  void createShowsFormWhenValidationErrors() {
    AssetCategoryMasterForm form = new AssetCategoryMasterForm("CAT01", "資産分類", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetCategoryMasterForm");
    bindingResult.rejectValue("code", "required", "入力必須");

    String view =
        controller.create(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("master/asset-category-form");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void createRejectsDuplicateCode() {
    AssetCategoryMasterForm form = new AssetCategoryMasterForm("CAT01", "資産分類", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetCategoryMasterForm");
    when(assetCategoryMasterService.isDuplicateCodeForCreate(form.code())).thenReturn(true);

    String view =
        controller.create(
            form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("master/asset-category-form");
    assertThat(bindingResult.getFieldError("code")).isNotNull();
    assertThat(bindingResult.getFieldError("code").getCode()).isEqualTo("duplicate");
    assertThat(bindingResult.getFieldError("code").getDefaultMessage())
        .isEqualTo("資産分類コードは既に使用されています。");
  }

  @Test
  void createRedirectsAfterSuccess() {
    AssetCategoryMasterForm form = new AssetCategoryMasterForm("CAT01", "資産分類", 1);
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.create(
            form,
            new BeanPropertyBindingResult(form, "assetCategoryMasterForm"),
            new ExtendedModelMap(),
            redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/asset-categories");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産分類を登録しました。");
  }

  @Test
  void editFormShowsFormModel() {
    when(assetCategoryMasterService.findForEdit(1L))
        .thenReturn(assetCategoryMasterDetail(1L, "CAT01"));
    String view = controller.editForm(1L, new ExtendedModelMap());

    assertThat(view).isEqualTo("master/asset-category-form");
  }

  @Test
  void updateShowsFormWhenValidationErrors() {
    AssetCategoryMasterForm form = new AssetCategoryMasterForm("CAT01", "資産分類", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetCategoryMasterForm");
    bindingResult.rejectValue("name", "required", "入力必須");
    when(assetCategoryMasterService.findForEdit(1L))
        .thenReturn(assetCategoryMasterDetail(1L, "CAT01"));

    String view =
        controller.update(
            1L, form, bindingResult, new ExtendedModelMap(), new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("master/asset-category-form");
    assertThat(bindingResult.hasErrors()).isTrue();
    verify(assetCategoryMasterService, never()).update(1L, form);
  }

  @Test
  void updateRedirectsAfterSuccess() {
    AssetCategoryMasterForm form = new AssetCategoryMasterForm("CAT01", "資産分類", 1);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "assetCategoryMasterForm");
    when(assetCategoryMasterService.findForEdit(1L))
        .thenReturn(assetCategoryMasterDetail(1L, "CAT01"));
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view =
        controller.update(1L, form, bindingResult, new ExtendedModelMap(), redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/asset-categories");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産分類を更新しました。");
  }

  @Test
  void deleteRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.delete(1L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/asset-categories");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産分類を削除しました。");
  }

  @Test
  void restoreRedirectsAfterSuccess() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    String view = controller.restore(1L, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/masters/asset-categories");
    assertThat(new java.util.HashMap<String, Object>(redirectAttributes.getFlashAttributes()))
        .containsEntry("message", "資産分類を復活しました。");
  }

  private AssetCategoryMasterListItem assetCategoryMasterListItem(Long id, String code) {
    return new AssetCategoryMasterListItem(
        id,
        code,
        "分類名",
        1,
        false,
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }

  private AssetCategoryMasterDetail assetCategoryMasterDetail(Long id, String code) {
    return new AssetCategoryMasterDetail(
        id,
        code,
        "分類名",
        1,
        false,
        LocalDateTime.of(2026, 1, 1, 9, 0),
        LocalDateTime.of(2026, 1, 1, 9, 0));
  }
}
