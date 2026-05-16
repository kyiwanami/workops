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
import com.example.workops.admin.department.form.DepartmentSearchForm;
import com.example.workops.admin.department.model.DepartmentListItem;
import com.example.workops.admin.department.model.DepartmentListPage;
import com.example.workops.admin.department.service.DepartmentAdminService;

/**
 * 部署一覧・登録・編集・削除画面を表示するController。
 */
@Controller
public class DepartmentAdminController {

    private final DepartmentAdminService departmentAdminService;

    public DepartmentAdminController(DepartmentAdminService departmentAdminService) {
        this.departmentAdminService = departmentAdminService;
    }

    @GetMapping("/admin/companies/{companyId}/departments")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformList(
            @PathVariable Long companyId,
            @ModelAttribute("departmentSearchForm") DepartmentSearchForm departmentSearchForm,
            Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findPlatformDepartmentList(
                companyId,
                departmentSearchForm);
        prepareListModel(model, departmentListPage, true);
        return "admin/department/department-list";
    }

    @GetMapping("/admin/companies/{companyId}/departments/new")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformNewForm(@PathVariable Long companyId, Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findPlatformDepartmentFormPage(companyId);
        model.addAttribute("departmentForm", DepartmentForm.empty());
        prepareFormModel(model, departmentListPage, true, false, null);
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
            prepareFormModel(model, departmentListPage, true, false, null);
            return "admin/department/department-form";
        }

        try {
            departmentAdminService.createPlatformDepartment(companyId, departmentForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                bindingResult.rejectValue("code", "duplicate", "部署コードは既に使用されています。");
                prepareFormModel(model, departmentListPage, true, false, null);
                return "admin/department/department-form";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "部署を登録しました。");
        return "redirect:/admin/companies/{companyId}/departments";
    }

    @GetMapping("/admin/companies/{companyId}/departments/{departmentId}/edit")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformEditForm(
            @PathVariable Long companyId,
            @PathVariable Long departmentId,
            Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findPlatformDepartmentFormPage(companyId);
        DepartmentListItem department = departmentAdminService.findPlatformDepartmentForEdit(companyId, departmentId);
        model.addAttribute("departmentForm", DepartmentForm.from(department));
        prepareFormModel(model, departmentListPage, true, true, departmentId);
        return "admin/department/department-form";
    }

    @PostMapping("/admin/companies/{companyId}/departments/{departmentId}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformUpdate(
            @PathVariable Long companyId,
            @PathVariable Long departmentId,
            @Valid @ModelAttribute("departmentForm") DepartmentForm departmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        DepartmentListPage departmentListPage = departmentAdminService.findPlatformDepartmentFormPage(companyId);
        departmentAdminService.findPlatformDepartmentForEdit(companyId, departmentId);
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, departmentListPage, true, true, departmentId);
            return "admin/department/department-form";
        }

        departmentAdminService.updatePlatformDepartment(companyId, departmentId, departmentForm);
        redirectAttributes.addFlashAttribute("message", "部署を更新しました。");
        return "redirect:/admin/companies/{companyId}/departments";
    }

    @PostMapping("/admin/companies/{companyId}/departments/{departmentId}/delete")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformDelete(
            @PathVariable Long companyId,
            @PathVariable Long departmentId,
            RedirectAttributes redirectAttributes) {
        departmentAdminService.deletePlatformDepartment(companyId, departmentId);
        redirectAttributes.addFlashAttribute("message", "部署を削除しました。");
        return "redirect:/admin/companies/{companyId}/departments";
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantList(
            @ModelAttribute("departmentSearchForm") DepartmentSearchForm departmentSearchForm,
            Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findTenantDepartmentList(departmentSearchForm);
        prepareListModel(model, departmentListPage, false);
        return "admin/department/department-list";
    }

    @GetMapping("/departments/new")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantNewForm(Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findTenantDepartmentFormPage();
        model.addAttribute("departmentForm", DepartmentForm.empty());
        prepareFormModel(model, departmentListPage, false, false, null);
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
            prepareFormModel(model, departmentListPage, false, false, null);
            return "admin/department/department-form";
        }

        try {
            departmentAdminService.createTenantDepartment(departmentForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                bindingResult.rejectValue("code", "duplicate", "部署コードは既に使用されています。");
                prepareFormModel(model, departmentListPage, false, false, null);
                return "admin/department/department-form";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "部署を登録しました。");
        return "redirect:/departments";
    }

    @GetMapping("/departments/{departmentId}/edit")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantEditForm(@PathVariable Long departmentId, Model model) {
        DepartmentListPage departmentListPage = departmentAdminService.findTenantDepartmentFormPage();
        DepartmentListItem department = departmentAdminService.findTenantDepartmentForEdit(departmentId);
        model.addAttribute("departmentForm", DepartmentForm.from(department));
        prepareFormModel(model, departmentListPage, false, true, departmentId);
        return "admin/department/department-form";
    }

    @PostMapping("/departments/{departmentId}")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantUpdate(
            @PathVariable Long departmentId,
            @Valid @ModelAttribute("departmentForm") DepartmentForm departmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        DepartmentListPage departmentListPage = departmentAdminService.findTenantDepartmentFormPage();
        departmentAdminService.findTenantDepartmentForEdit(departmentId);
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, departmentListPage, false, true, departmentId);
            return "admin/department/department-form";
        }

        departmentAdminService.updateTenantDepartment(departmentId, departmentForm);
        redirectAttributes.addFlashAttribute("message", "部署を更新しました。");
        return "redirect:/departments";
    }

    @PostMapping("/departments/{departmentId}/delete")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantDelete(@PathVariable Long departmentId, RedirectAttributes redirectAttributes) {
        departmentAdminService.deleteTenantDepartment(departmentId);
        redirectAttributes.addFlashAttribute("message", "部署を削除しました。");
        return "redirect:/departments";
    }

    private void prepareListModel(Model model, DepartmentListPage departmentListPage, boolean platformAdmin) {
        model.addAttribute("departmentListPage", departmentListPage);
        model.addAttribute("platformAdmin", platformAdmin);
    }

    private void prepareFormModel(
            Model model,
            DepartmentListPage departmentListPage,
            boolean platformAdmin,
            boolean edit,
            Long departmentId) {
        String listPath = "/departments";
        if (platformAdmin) {
            listPath = "/admin/companies/" + departmentListPage.companyId() + "/departments";
        }
        String formAction = listPath;
        if (edit) {
            formAction = listPath + "/" + departmentId;
        }
        model.addAttribute("departmentListPage", departmentListPage);
        model.addAttribute("platformAdmin", platformAdmin);
        model.addAttribute("edit", edit);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("formAction", formAction);
        model.addAttribute("cancelHref", listPath);
    }
}
