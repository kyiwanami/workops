package com.example.workops.admin.user.web;

import java.util.List;

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

import com.example.workops.admin.user.form.UserEditForm;
import com.example.workops.admin.user.form.UserForm;
import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserEditTarget;
import com.example.workops.admin.user.model.UserListItem;
import com.example.workops.admin.user.service.UserAdminService;

/**
 * ユーザー一覧・詳細画面を表示するController。
 */
@Controller
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformList(Model model) {
        List<UserListItem> users = userAdminService.findPlatformUsers();
        prepareListModel(model, users, true);
        return "admin/user/user-list";
    }

    @GetMapping("/admin/users/new")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformNewForm(Model model) {
        model.addAttribute("userForm", UserForm.empty());
        preparePlatformFormModel(model);
        return "admin/user/user-form";
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformCreate(
            @Valid @ModelAttribute("userForm") UserForm userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preparePlatformFormModel(model);
            return "admin/user/user-form";
        }

        Long userId;
        try {
            userId = userAdminService.createPlatformUser(userForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                rejectCreateError(bindingResult, exception);
                preparePlatformFormModel(model);
                return "admin/user/user-form";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "ユーザーを登録しました。");
        return "redirect:/admin/users/" + userId;
    }

    @GetMapping("/admin/users/{userId}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformDetail(@PathVariable Long userId, Model model) {
        UserDetail user = userAdminService.findPlatformUserDetail(userId);
        prepareDetailModel(model, user, true);
        return "admin/user/user-detail";
    }

    @GetMapping("/admin/users/{userId}/edit")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformEditForm(@PathVariable Long userId, Model model) {
        UserEditTarget user = userAdminService.findPlatformUserEditTarget(userId);
        model.addAttribute("userEditForm", userAdminService.findPlatformUserEditForm(userId));
        preparePlatformEditModel(model, user);
        return "admin/user/user-edit";
    }

    @PostMapping("/admin/users/{userId}/edit")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformUpdate(
            @PathVariable Long userId,
            @Valid @ModelAttribute("userEditForm") UserEditForm userEditForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        UserEditTarget user = userAdminService.findPlatformUserEditTarget(userId);
        if (bindingResult.hasErrors()) {
            preparePlatformEditModel(model, user);
            return "admin/user/user-edit";
        }

        try {
            userAdminService.updatePlatformUser(userId, userEditForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                rejectEditError(bindingResult, exception);
                preparePlatformEditModel(model, user);
                return "admin/user/user-edit";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "ユーザーを更新しました。");
        return "redirect:/admin/users/" + userId;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantList(Model model) {
        List<UserListItem> users = userAdminService.findTenantUsers();
        prepareListModel(model, users, false);
        return "admin/user/user-list";
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantNewForm(Model model) {
        model.addAttribute("userForm", UserForm.empty());
        prepareTenantFormModel(model);
        return "admin/user/user-form";
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantCreate(
            @Valid @ModelAttribute("userForm") UserForm userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareTenantFormModel(model);
            return "admin/user/user-form";
        }

        Long userId;
        try {
            userId = userAdminService.createTenantUser(userForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                rejectCreateError(bindingResult, exception);
                prepareTenantFormModel(model);
                return "admin/user/user-form";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "ユーザーを登録しました。");
        return "redirect:/users/" + userId;
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantDetail(@PathVariable Long userId, Model model) {
        UserDetail user = userAdminService.findTenantUserDetail(userId);
        prepareDetailModel(model, user, false);
        return "admin/user/user-detail";
    }

    @GetMapping("/users/{userId}/edit")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantEditForm(@PathVariable Long userId, Model model) {
        UserEditTarget user = userAdminService.findTenantUserEditTarget(userId);
        model.addAttribute("userEditForm", userAdminService.findTenantUserEditForm(userId));
        prepareTenantEditModel(model, user);
        return "admin/user/user-edit";
    }

    @PostMapping("/users/{userId}/edit")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantUpdate(
            @PathVariable Long userId,
            @Valid @ModelAttribute("userEditForm") UserEditForm userEditForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        UserEditTarget user = userAdminService.findTenantUserEditTarget(userId);
        if (bindingResult.hasErrors()) {
            prepareTenantEditModel(model, user);
            return "admin/user/user-edit";
        }

        try {
            userAdminService.updateTenantUser(userId, userEditForm);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                rejectEditError(bindingResult, exception);
                prepareTenantEditModel(model, user);
                return "admin/user/user-edit";
            }
            throw exception;
        }

        redirectAttributes.addFlashAttribute("message", "ユーザーを更新しました。");
        return "redirect:/users/" + userId;
    }

    private void prepareListModel(Model model, List<UserListItem> users, boolean platformAdmin) {
        model.addAttribute("users", users);
        model.addAttribute("platformAdmin", platformAdmin);
    }

    private void preparePlatformFormModel(Model model) {
        model.addAttribute("formMode", "platform");
        model.addAttribute("formAction", "/admin/users");
        model.addAttribute("cancelHref", "/admin/users");
        model.addAttribute("companies", userAdminService.findActiveCompanies());
        model.addAttribute("departments", userAdminService.findActiveDepartments());
        model.addAttribute("platformPermissionSets", userAdminService.findPlatformPermissionSetOptions());
        model.addAttribute("tenantPermissionSets", userAdminService.findTenantPermissionSetOptions());
    }

    private void prepareTenantFormModel(Model model) {
        model.addAttribute("formMode", "tenant");
        model.addAttribute("formAction", "/users");
        model.addAttribute("cancelHref", "/users");
        model.addAttribute("departments", userAdminService.findTenantActiveDepartments());
        model.addAttribute("tenantPermissionSets", userAdminService.findTenantPermissionSetOptions());
    }

    private void prepareDetailModel(Model model, UserDetail user, boolean platformAdmin) {
        model.addAttribute("user", user);
        model.addAttribute("platformAdmin", platformAdmin);
        model.addAttribute("listHref", platformAdmin ? "/admin/users" : "/users");
        model.addAttribute("editHref", platformAdmin ? "/admin/users/" + user.id() + "/edit" : "/users/" + user.id() + "/edit");
    }

    private void preparePlatformEditModel(Model model, UserEditTarget user) {
        model.addAttribute("user", user);
        model.addAttribute("formAction", "/admin/users/" + user.id() + "/edit");
        model.addAttribute("cancelHref", "/admin/users/" + user.id());
        model.addAttribute("departments", userAdminService.findActiveDepartmentsByCompanyId(user.companyId()));
        if ("PLATFORM".equals(user.actorType())) {
            model.addAttribute("permissionSets", userAdminService.findPlatformPermissionSetOptions());
        } else {
            model.addAttribute("permissionSets", userAdminService.findTenantPermissionSetOptions());
        }
    }

    private void prepareTenantEditModel(Model model, UserEditTarget user) {
        model.addAttribute("user", user);
        model.addAttribute("formAction", "/users/" + user.id() + "/edit");
        model.addAttribute("cancelHref", "/users/" + user.id());
        model.addAttribute("departments", userAdminService.findTenantActiveDepartments());
        model.addAttribute("permissionSets", userAdminService.findTenantPermissionSetOptions());
    }

    private void rejectCreateError(BindingResult bindingResult, ResponseStatusException exception) {
        String reason = exception.getReason();
        if ("ユーザー名は既に使用されています。".equals(reason)) {
            bindingResult.rejectValue("username", "duplicate", reason);
            return;
        }
        if ("emailは既に使用されています。".equals(reason)) {
            bindingResult.rejectValue("email", "duplicate", reason);
            return;
        }
        if ("会社を選択してください。".equals(reason) || "会社が見つかりません。".equals(reason)) {
            bindingResult.rejectValue("companyId", "invalid", reason);
            return;
        }
        if ("所属部署が見つかりません。".equals(reason)) {
            bindingResult.rejectValue("departmentId", "invalid", reason);
            return;
        }
        if ("actor_typeが不正です。".equals(reason)) {
            bindingResult.rejectValue("actorType", "invalid", reason);
            return;
        }
        bindingResult.reject("invalid", reason);
    }

    private void rejectEditError(BindingResult bindingResult, ResponseStatusException exception) {
        String reason = exception.getReason();
        if ("emailは既に使用されています。".equals(reason)) {
            bindingResult.rejectValue("email", "duplicate", reason);
            return;
        }
        if ("所属部署が見つかりません。".equals(reason) || "PLATFORMユーザーには部署を指定できません。".equals(reason)) {
            bindingResult.rejectValue("departmentId", "invalid", reason);
            return;
        }
        if ("権限セットを選択してください。".equals(reason)
                || "PLATFORMユーザーの権限セットが不正です。".equals(reason)
                || "TENANTユーザーの権限セットが不正です。".equals(reason)
                || "TENANT_MANAGERは最低1人必要です。".equals(reason)) {
            bindingResult.rejectValue("permissionSetCodes", "invalid", reason);
            return;
        }
        bindingResult.reject("invalid", reason);
    }
}
