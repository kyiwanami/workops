package com.example.workops.request.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 却下・差戻し理由の入力を保持する。 */
public record RequestReviewForm(
    @NotBlank(message = "理由を入力してください。") @Size(max = 1000, message = "理由は1000文字以内で入力してください。")
        String reviewComment) {

  public static RequestReviewForm empty() {
    return new RequestReviewForm(null);
  }
}
