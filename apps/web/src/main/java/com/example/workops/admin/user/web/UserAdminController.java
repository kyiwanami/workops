package com.example.workops.admin.user.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.workops.admin.user.model.UserDetail;
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

    @GetMapping("/admin/users/{userId}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public String platformDetail(@PathVariable Long userId, Model model) {
        UserDetail user = userAdminService.findPlatformUserDetail(userId);
        prepareDetailModel(model, user, true);
        return "admin/user/user-detail";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantList(Model model) {
        List<UserListItem> users = userAdminService.findTenantUsers();
        prepareListModel(model, users, false);
        return "admin/user/user-list";
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String tenantDetail(@PathVariable Long userId, Model model) {
        UserDetail user = userAdminService.findTenantUserDetail(userId);
        prepareDetailModel(model, user, false);
        return "admin/user/user-detail";
    }

    private void prepareListModel(Model model, List<UserListItem> users, boolean platformAdmin) {
        model.addAttribute("users", users);
        model.addAttribute("platformAdmin", platformAdmin);
    }

    private void prepareDetailModel(Model model, UserDetail user, boolean platformAdmin) {
        model.addAttribute("user", user);
        model.addAttribute("platformAdmin", platformAdmin);
        model.addAttribute("listHref", platformAdmin ? "/admin/users" : "/users");
    }
}
