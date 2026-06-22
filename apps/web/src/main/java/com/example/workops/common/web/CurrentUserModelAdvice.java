package com.example.workops.common.web;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 共通layoutが現在ユーザーの有無に応じて認証導線を表示できるようにする。 */
@ControllerAdvice
public class CurrentUserModelAdvice {

  private final CurrentUserProvider currentUserProvider;

  public CurrentUserModelAdvice(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  @ModelAttribute("currentUser")
  public LoginUserContext currentUser() {
    return currentUserProvider.currentUser().orElse(null);
  }
}
