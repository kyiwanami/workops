package com.example.workops.admin.user.service;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * local profileでCognitoユーザー作成を代替し、DB登録用の疑似 {@code sub} を返す。
 *
 * <p>通常の自動テストとローカル画面確認ではAWSへ接続せず、この実装がUUID形式の {@code cognito_sub} を発行する。
 */
@Service
@Profile("local")
public class LocalCognitoUserProvisioner implements CognitoUserProvisioner {

  /**
   * Cognito APIを呼び出さず、WorkOps DB登録用の疑似 {@code cognito_sub} を発行する。
   *
   * @param request local profileでは値の検証やAWS送信に使わない入力
   * @return UUID形式の疑似 {@code cognito_sub}
   */
  @Override
  public ProvisionedCognitoUser provision(CognitoUserProvisionRequest request) {
    return new ProvisionedCognitoUser(UUID.randomUUID().toString());
  }
}
