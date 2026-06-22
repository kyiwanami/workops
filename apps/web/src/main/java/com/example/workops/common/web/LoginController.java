package com.example.workops.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 未認証利用者向けのログイン入口を提供するController。 */
@Controller
public class LoginController {

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/login/platform")
  public String platformLogin() {
    return "redirect:/oauth2/authorization/platform";
  }

  @GetMapping("/login/tenant")
  public String tenantLogin() {
    return "redirect:/oauth2/authorization/tenant";
  }
}
