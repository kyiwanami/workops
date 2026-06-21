package com.example.workops.common.web;

import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;

/**
 * MVP画面実装の起点になるトップ画面を表示するController。
 */
@Controller
public class HomeController {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String TENANT_VIEWER = "TENANT_VIEWER";
    private static final String TENANT_EDITOR = "TENANT_EDITOR";
    private static final String TENANT_MANAGER = "TENANT_MANAGER";
    private static final Set<String> TENANT_VIEW_PERMISSIONS = Set.of(TENANT_VIEWER, TENANT_EDITOR, TENANT_MANAGER);

    private final CurrentUserProvider currentUserProvider;

    public HomeController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/")
    public String index(Model model) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        boolean canViewTenantBusiness = hasAnyPermission(currentUser, TENANT_VIEW_PERMISSIONS);
        boolean canManageTenant = hasPermission(currentUser, TENANT_MANAGER);
        boolean canManagePlatform = hasPermission(currentUser, PLATFORM_ADMIN);

        // Top-page links mirror each target controller's @PreAuthorize expression.
        model.addAttribute("canViewRequests", canViewTenantBusiness);
        model.addAttribute("canViewAssets", canViewTenantBusiness);
        model.addAttribute("canManageRequestTypes", canManageTenant);
        model.addAttribute("canManageAssetCategories", canManageTenant);
        model.addAttribute("canManagePlatformCompanies", canManagePlatform);
        model.addAttribute("canManagePlatformDepartments", canManagePlatform);
        model.addAttribute("canManageTenantDepartments", canManageTenant);
        model.addAttribute("canManagePlatformUsers", canManagePlatform);
        model.addAttribute("canManageTenantUsers", canManageTenant);
        return "index";
    }

    private boolean hasPermission(LoginUserContext currentUser, String permissionCode) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> permissionSet.code().equals(permissionCode));
    }

    private boolean hasAnyPermission(LoginUserContext currentUser, Set<String> permissionCodes) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> permissionCodes.contains(permissionSet.code()));
    }
}
