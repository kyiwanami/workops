package com.example.workops.common.web;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;

/**
 * Cognito OAuth2 Login後にSpring Securityが保持するOIDC claimを確認するController。
 */
@Controller
public class AuthClaimsController {

    private final CurrentUserProvider currentUserProvider;

    public AuthClaimsController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/auth/claims")
    public String claims(Authentication authentication, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            List<ClaimRow> claimRows = List.of(
                    new ClaimRow("sub", oidcUser.getSubject()));
            LoginUserContext loginUserContext = currentUserProvider.currentUser().orElse(null);

            model.addAttribute("authenticated", true);
            model.addAttribute("principalName", authentication.getName());
            model.addAttribute("claimRows", claimRows);
            model.addAttribute("loginUserContext", loginUserContext);
            return "auth/claims";
        }

        model.addAttribute("authenticated", false);
        model.addAttribute("principalName", "");
        model.addAttribute("claimRows", List.of());
        model.addAttribute("loginUserContext", null);
        return "auth/claims";
    }

    public record ClaimRow(String name, String value) {
    }
}
