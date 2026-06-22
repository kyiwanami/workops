package com.example.workops.common.web;

import com.example.workops.common.logging.ApplicationExceptionLogRecord;
import com.example.workops.common.logging.ApplicationExceptionLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Spring MVCのアプリ例外をログへ残し、画面向けの共通エラー表示へ変換する。 */
@ControllerAdvice
public class ApplicationExceptionHandler {

  private static final String EVENT_TYPE_APPLICATION_EXCEPTION = "APPLICATION_EXCEPTION";
  private static final String ERROR_VIEW = "error";

  private final ApplicationExceptionLogger applicationExceptionLogger;

  public ApplicationExceptionHandler(ApplicationExceptionLogger applicationExceptionLogger) {
    this.applicationExceptionLogger = applicationExceptionLogger;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public String handleResponseStatusException(
      ResponseStatusException exception,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    int status = exception.getStatusCode().value();
    return handleApplicationException(exception, request, response, model, status);
  }

  @ExceptionHandler(AuthenticationException.class)
  public void handleAuthenticationException(AuthenticationException exception)
      throws AuthenticationException {
    throw exception;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public void handleAccessDeniedException(AccessDeniedException exception)
      throws AccessDeniedException {
    throw exception;
  }

  @ExceptionHandler(Exception.class)
  public String handleException(
      Exception exception, HttpServletRequest request, HttpServletResponse response, Model model) {
    return handleApplicationException(
        exception, request, response, model, HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  private String handleApplicationException(
      Exception exception,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model,
      int status) {
    applicationExceptionLogger.log(
        new ApplicationExceptionLogRecord(
            EVENT_TYPE_APPLICATION_EXCEPTION,
            status,
            request.getRequestURI(),
            exception.getClass().getSimpleName()),
        exception);
    response.setStatus(status);
    model.addAttribute("status", status);
    model.addAttribute("requestId", applicationExceptionLogger.requestId());
    return ERROR_VIEW;
  }
}
