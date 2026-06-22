package com.example.workops.master.web;

import com.example.workops.master.form.AssetCategoryMasterForm;
import com.example.workops.master.form.AssetCategoryMasterSearchForm;
import com.example.workops.master.model.AssetCategoryMasterDetail;
import com.example.workops.master.service.AssetCategoryMasterService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 資産分類マスタ管理画面を表示するController。 */
@Controller
public class AssetCategoryMasterController {

  private final AssetCategoryMasterService assetCategoryMasterService;

  public AssetCategoryMasterController(AssetCategoryMasterService assetCategoryMasterService) {
    this.assetCategoryMasterService = assetCategoryMasterService;
  }

  @GetMapping("/masters/asset-categories")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String list(
      @ModelAttribute("assetCategoryMasterSearchForm")
          AssetCategoryMasterSearchForm assetCategoryMasterSearchForm,
      Model model) {
    model.addAttribute(
        "assetCategories", assetCategoryMasterService.findList(assetCategoryMasterSearchForm));
    return "master/asset-category-list";
  }

  @GetMapping("/masters/asset-categories/new")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String newForm(Model model) {
    model.addAttribute("assetCategoryMasterForm", AssetCategoryMasterForm.empty());
    prepareFormModel(model, false, null);
    return "master/asset-category-form";
  }

  @PostMapping("/masters/asset-categories")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String create(
      @Valid @ModelAttribute("assetCategoryMasterForm")
          AssetCategoryMasterForm assetCategoryMasterForm,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      prepareFormModel(model, false, null);
      return "master/asset-category-form";
    }
    if (assetCategoryMasterService.isDuplicateCodeForCreate(assetCategoryMasterForm.code())) {
      bindingResult.rejectValue("code", "duplicate", "資産分類コードは既に使用されています。");
      prepareFormModel(model, false, null);
      return "master/asset-category-form";
    }

    assetCategoryMasterService.create(assetCategoryMasterForm);
    redirectAttributes.addFlashAttribute("message", "資産分類を登録しました。");
    return "redirect:/masters/asset-categories";
  }

  @GetMapping("/masters/asset-categories/{id}/edit")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String editForm(@PathVariable Long id, Model model) {
    AssetCategoryMasterDetail assetCategory = assetCategoryMasterService.findForEdit(id);
    model.addAttribute("assetCategoryMasterForm", AssetCategoryMasterForm.from(assetCategory));
    prepareFormModel(model, true, id);
    return "master/asset-category-form";
  }

  @PostMapping("/masters/asset-categories/{id}")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("assetCategoryMasterForm")
          AssetCategoryMasterForm assetCategoryMasterForm,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    assetCategoryMasterService.findForEdit(id);
    if (bindingResult.hasErrors()) {
      prepareFormModel(model, true, id);
      return "master/asset-category-form";
    }

    assetCategoryMasterService.update(id, assetCategoryMasterForm);
    redirectAttributes.addFlashAttribute("message", "資産分類を更新しました。");
    return "redirect:/masters/asset-categories";
  }

  @PostMapping("/masters/asset-categories/{id}/delete")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    assetCategoryMasterService.delete(id);
    redirectAttributes.addFlashAttribute("message", "資産分類を削除しました。");
    return "redirect:/masters/asset-categories";
  }

  @PostMapping("/masters/asset-categories/{id}/restore")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    assetCategoryMasterService.restore(id);
    redirectAttributes.addFlashAttribute("message", "資産分類を復活しました。");
    return "redirect:/masters/asset-categories";
  }

  private void prepareFormModel(Model model, boolean edit, Long assetCategoryId) {
    model.addAttribute("edit", edit);
    model.addAttribute("assetCategoryId", assetCategoryId);
  }
}
