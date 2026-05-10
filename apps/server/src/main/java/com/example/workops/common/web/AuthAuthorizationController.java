package com.example.workops.common.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.workops.common.security.LoginUserContext;

/**
 * Webページ単位でSpring Security method securityの動作を確認するController。
 */
@Controller
public class AuthAuthorizationController {

    @GetMapping("/auth/authorization/manager")
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public String managerOnly(@AuthenticationPrincipal LoginUserContext currentUser, Model model) {
        model.addAttribute("loginUserContext", currentUser);
        return "auth/authorization-manager";
    }
}
