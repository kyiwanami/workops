package com.example.workops.asset.web;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.workops.asset.form.AssetForm;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.asset.service.AssetCommandService;
import com.example.workops.asset.service.AssetQueryService;

/**
 * 資産一覧と資産詳細を表示するController。
 */
@Controller
public class AssetController {

    private final AssetQueryService assetQueryService;
    private final AssetCommandService assetCommandService;

    public AssetController(AssetQueryService assetQueryService, AssetCommandService assetCommandService) {
        this.assetQueryService = assetQueryService;
        this.assetCommandService = assetCommandService;
    }

    @GetMapping("/assets")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String list(Model model) {
        model.addAttribute("assets", assetQueryService.findList());
        model.addAttribute("canCreate", assetQueryService.canCreateAsset());
        return "asset/list";
    }

    @GetMapping("/assets/new")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String newForm(Model model) {
        model.addAttribute("assetForm", AssetForm.empty());
        prepareFormModel(model, false, null);
        return "asset/form";
    }

    @PostMapping("/assets")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String create(
            @Valid @ModelAttribute("assetForm") AssetForm assetForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, false, null);
            return "asset/form";
        }
        if (assetCommandService.isDuplicateCodeForCreate(assetForm.code())) {
            bindingResult.rejectValue("code", "duplicate", "資産コードは既に使用されています。");
            prepareFormModel(model, false, null);
            return "asset/form";
        }

        Long assetId = assetCommandService.createAsset(assetForm);
        redirectAttributes.addFlashAttribute("message", "資産を登録しました。");
        return "redirect:/assets/" + assetId;
    }

    @GetMapping("/assets/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_VIEWER','TENANT_EDITOR','TENANT_MANAGER')")
    public String detail(@PathVariable Long id, Model model) {
        AssetDetail asset = assetQueryService.findDetail(id);
        model.addAttribute("asset", asset);
        model.addAttribute("canEdit", assetQueryService.canEditAsset(asset));
        return "asset/detail";
    }

    @GetMapping("/assets/{id}/edit")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String editForm(@PathVariable Long id, Model model) {
        AssetDetail asset = assetCommandService.findAssetForEdit(id);
        model.addAttribute("assetForm", AssetForm.from(asset));
        prepareFormModel(model, true, id);
        return "asset/form";
    }

    @PostMapping("/assets/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("assetForm") AssetForm assetForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        assetCommandService.findAssetForEdit(id);
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, true, id);
            return "asset/form";
        }
        if (assetCommandService.isDuplicateCodeForUpdate(id, assetForm.code())) {
            bindingResult.rejectValue("code", "duplicate", "資産コードは既に使用されています。");
            prepareFormModel(model, true, id);
            return "asset/form";
        }

        assetCommandService.updateAsset(id, assetForm);
        redirectAttributes.addFlashAttribute("message", "資産を更新しました。");
        return "redirect:/assets/" + id;
    }

    private void prepareFormModel(Model model, boolean edit, Long assetId) {
        model.addAttribute("assetCategoryOptions", assetQueryService.findAssetCategoryOptions());
        model.addAttribute("departmentOptions", assetQueryService.findDepartmentOptions());
        model.addAttribute("statusOptions", assetQueryService.findStatusOptions());
        model.addAttribute("edit", edit);
        model.addAttribute("assetId", assetId);
    }
}
