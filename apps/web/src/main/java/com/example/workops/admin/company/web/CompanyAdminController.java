package com.example.workops.admin.company.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.workops.admin.company.form.CompanyForm;
import com.example.workops.admin.company.service.CompanyAdminService;

/**
 * PLATFORM_ADMIN向け会社作成画面を表示するController。
 */
@Controller
public class CompanyAdminController {

    private final CompanyAdminService companyAdminService;

    public CompanyAdminController(CompanyAdminService companyAdminService) {
        this.companyAdminService = companyAdminService;
    }

    @GetMapping("/admin/companies/new")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("companyForm", CompanyForm.empty());
        return "admin/company/company-form";
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
