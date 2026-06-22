package com.example.workops.admin.company.web;

import com.example.workops.admin.company.form.CompanyEditForm;
import com.example.workops.admin.company.form.CompanyForm;
import com.example.workops.admin.company.form.CompanySearchForm;
import com.example.workops.admin.company.model.CompanyDetail;
import com.example.workops.admin.company.model.CompanyListItem;
import com.example.workops.admin.company.service.CompanyAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** PLATFORM_ADMIN向け会社管理画面を表示するController。 */
@Controller
public class CompanyAdminController {

  private final CompanyAdminService companyAdminService;

  public CompanyAdminController(CompanyAdminService companyAdminService) {
    this.companyAdminService = companyAdminService;
  }

  @GetMapping("/admin/companies")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public String list(
      @ModelAttribute("companySearchForm") CompanySearchForm companySearchForm, Model model) {
    List<CompanyListItem> companies = companyAdminService.findCompanies(companySearchForm);
    model.addAttribute("companies", companies);
    return "admin/company/company-list";
  }

  @GetMapping("/admin/companies/{companyId}")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public String detail(@PathVariable Long companyId, Model model) {
    CompanyDetail company = companyAdminService.findCompanyDetail(companyId);
    model.addAttribute("company", company);
    return "admin/company/company-detail";
  }

  @GetMapping("/admin/companies/new")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public String newForm(Model model) {
    model.addAttribute("companyForm", CompanyForm.empty());
    return "admin/company/company-form";
  }

  @GetMapping("/admin/companies/{companyId}/edit")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public String editForm(@PathVariable Long companyId, Model model) {
    CompanyEditForm companyEditForm = companyAdminService.findCompanyEditForm(companyId);
    prepareEditModel(model, companyId, companyEditForm);
    return "admin/company/company-edit";
  }

  @PostMapping("/admin/companies")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public String create(
      @Valid @ModelAttribute("companyForm") CompanyForm companyForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return "admin/company/company-form";
    }

    Long companyId;
    try {
      companyId = companyAdminService.create(companyForm);
    } catch (ResponseStatusException exception) {
      if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
        rejectCreateError(bindingResult, exception);
        return "admin/company/company-form";
      }
      throw exception;
    }

    redirectAttributes.addFlashAttribute("message", "会社を登録しました。");
    return "redirect:/admin/companies/" + companyId + "/departments";
  }

  @PostMapping("/admin/companies/{companyId}/edit")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public String update(
      @PathVariable Long companyId,
      @Valid @ModelAttribute("companyEditForm") CompanyEditForm companyEditForm,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    companyAdminService.findCompanyEditForm(companyId);
    if (bindingResult.hasErrors()) {
      prepareEditModel(model, companyId, companyEditForm);
      return "admin/company/company-edit";
    }

    companyAdminService.updateCompany(companyId, companyEditForm);
    redirectAttributes.addFlashAttribute("message", "会社を更新しました。");
    return "redirect:/admin/companies/{companyId}";
  }

  @PostMapping("/admin/companies/{companyId}/delete")
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public String delete(@PathVariable Long companyId, RedirectAttributes redirectAttributes) {
    companyAdminService.deleteCompany(companyId);
    redirectAttributes.addFlashAttribute("message", "会社を削除しました。");
    return "redirect:/admin/companies?showDeleted=true";
  }

  private void prepareEditModel(Model model, Long companyId, CompanyEditForm companyEditForm) {
    CompanyDetail company = companyAdminService.findCompanyDetail(companyId);
    model.addAttribute("company", company);
    model.addAttribute("companyEditForm", companyEditForm);
    model.addAttribute("formAction", "/admin/companies/" + companyId + "/edit");
    model.addAttribute("cancelHref", "/admin/companies/" + companyId);
  }

  private void rejectCreateError(BindingResult bindingResult, ResponseStatusException exception) {
    String reason = exception.getReason();
    if ("会社コードは既に使用されています。".equals(reason)) {
      bindingResult.rejectValue("code", "duplicate", reason);
      return;
    }
    if ("ユーザー名は既に使用されています。".equals(reason)) {
      bindingResult.rejectValue("initialTenantManagerUsername", "duplicate", reason);
      return;
    }
    if ("emailは既に使用されています。".equals(reason)) {
      bindingResult.rejectValue("initialTenantManagerEmail", "duplicate", reason);
      return;
    }
    bindingResult.reject("invalid", reason);
  }
}
