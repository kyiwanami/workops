package com.example.workops.admin.department.web;

import jakarta.validation.Valid;

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

import com.example.workops.admin.department.form.DepartmentForm;
import com.example.workops.admin.department.model.DepartmentListPage;
import com.example.workops.admin.department.service.DepartmentAdminService;

/**
 * 部署一覧と部署作成画面を表示するController。
 */
@Controller
public class DepartmentAdminController {

    private final DepartmentAdminService departmentAdminService;

    public DepartmentAdminController(DepartmentAdminService departmentAdminService) {
        this.departmentAdminService = departmentAdminService;
    }

    @GetMapping("/admin/companies/{companyId}/departments")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformList(@PathVariable Long companyId, Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findPlatformDepartmentList(companyId);
        prepareListModel(model, departmentListPage, true);
        return "admin/department/department-list";
    }

    @GetMapping("/admin/companies/{companyId}/departments/new")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformNewForm(@PathVariable Long companyId, Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findPlatformDepartmentFormPage(companyId);
        model.addAttribute("departmentForm", DepartmentForm.empty());
        prepareFormModel(model, departmentListPage, true);
        return "admin/department/department-form";
    }

    @PostMapping("/admin/companies/{companyId}/departments")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformCreate(
            @PathVariable Long companyId,
            @Valid @ModelAttribute("departmentForm") DepartmentForm departmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        DepartmentListPage departmentListPage = departmentAdminService.findPlatformDepartmentFormPage(companyId);
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, departmentListPage, true);
            return "admin/department/department-form";
        }

        try {
            departmentAdminService.createPlatformDepartment(companyId, departmentForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                bindingResult.rejectValue("code", "duplicate", "部署コードは既に使用されています。");
                prepareFormModel(model, departmentListPage, true);
                return "admin/department/department-form";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "部署を登録しました。");
        return "redirect:/admin/companies/{companyId}/departments";
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantList(Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findTenantDepartmentList();
        prepareListModel(model, departmentListPage, false);
        return "admin/department/department-list";
    }

    @GetMapping("/departments/new")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantNewForm(Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findTenantDepartmentFormPage();
        model.addAttribute("departmentForm", DepartmentForm.empty());
        prepareFormModel(model, departmentListPage, false);
        return "admin/department/department-form";
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantCreate(
            @Valid @ModelAttribute("departmentForm") DepartmentForm departmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        DepartmentListPage departmentListPage = departmentAdminService.findTenantDepartmentFormPage();
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, departmentListPage, false);
            return "admin/department/department-form";
        }

        try {
            departmentAdminService.createTenantDepartment(departmentForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                bindingResult.rejectValue("code", "duplicate", "部署コードは既に使用されています。");
                prepareFormModel(model, departmentListPage, false);
                return "admin/department/department-form";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "部署を登録しました。");
        return "redirect:/departments";
    }

    private void prepareListModel(Model model, DepartmentListPage departmentListPage, boolean platformAdmin) {
        model.addAttribute("departmentListPage", departmentListPage);
        model.addAttribute("platformAdmin", platformAdmin);
    }

    private void prepareFormModel(Model model, DepartmentListPage departmentListPage, boolean platformAdmin) {
        model.addAttribute("departmentListPage", departmentListPage);
        model.addAttribute("platformAdmin", platformAdmin);
    }
}
