package com.example.workops.master.web;

import com.example.workops.master.form.RequestTypeMasterForm;
import com.example.workops.master.form.RequestTypeMasterSearchForm;
import com.example.workops.master.model.RequestTypeMasterDetail;
import com.example.workops.master.service.RequestTypeMasterService;
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

/** 申請種別マスタ管理画面を表示するController。 */
@Controller
public class RequestTypeMasterController {

  private final RequestTypeMasterService requestTypeMasterService;

  public RequestTypeMasterController(RequestTypeMasterService requestTypeMasterService) {
    this.requestTypeMasterService = requestTypeMasterService;
  }

  @GetMapping("/masters/request-types")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String list(
      @ModelAttribute("requestTypeMasterSearchForm")
          RequestTypeMasterSearchForm requestTypeMasterSearchForm,
      Model model) {
    model.addAttribute(
        "requestTypes", requestTypeMasterService.findList(requestTypeMasterSearchForm));
    return "master/request-type-list";
  }

  @GetMapping("/masters/request-types/new")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String newForm(Model model) {
    model.addAttribute("requestTypeMasterForm", RequestTypeMasterForm.empty());
    prepareFormModel(model, false, null);
    return "master/request-type-form";
  }

  @PostMapping("/masters/request-types")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String create(
      @Valid @ModelAttribute("requestTypeMasterForm") RequestTypeMasterForm requestTypeMasterForm,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      prepareFormModel(model, false, null);
      return "master/request-type-form";
    }
    if (requestTypeMasterService.isDuplicateCodeForCreate(requestTypeMasterForm.code())) {
      bindingResult.rejectValue("code", "duplicate", "申請種別コードは既に使用されています。");
      prepareFormModel(model, false, null);
      return "master/request-type-form";
    }

    requestTypeMasterService.create(requestTypeMasterForm);
    redirectAttributes.addFlashAttribute("message", "申請種別を登録しました。");
    return "redirect:/masters/request-types";
  }

  @GetMapping("/masters/request-types/{id}/edit")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String editForm(@PathVariable Long id, Model model) {
    RequestTypeMasterDetail requestType = requestTypeMasterService.findForEdit(id);
    model.addAttribute("requestTypeMasterForm", RequestTypeMasterForm.from(requestType));
    prepareFormModel(model, true, id);
    return "master/request-type-form";
  }

  @PostMapping("/masters/request-types/{id}")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("requestTypeMasterForm") RequestTypeMasterForm requestTypeMasterForm,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    requestTypeMasterService.findForEdit(id);
    if (bindingResult.hasErrors()) {
      prepareFormModel(model, true, id);
      return "master/request-type-form";
    }

    requestTypeMasterService.update(id, requestTypeMasterForm);
    redirectAttributes.addFlashAttribute("message", "申請種別を更新しました。");
    return "redirect:/masters/request-types";
  }

  @PostMapping("/masters/request-types/{id}/delete")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    requestTypeMasterService.delete(id);
    redirectAttributes.addFlashAttribute("message", "申請種別を削除しました。");
    return "redirect:/masters/request-types";
  }

  @PostMapping("/masters/request-types/{id}/restore")
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    requestTypeMasterService.restore(id);
    redirectAttributes.addFlashAttribute("message", "申請種別を復活しました。");
    return "redirect:/masters/request-types";
  }

  private void prepareFormModel(Model model, boolean edit, Long requestTypeId) {
    model.addAttribute("edit", edit);
    model.addAttribute("requestTypeId", requestTypeId);
  }
}
