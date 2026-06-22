package com.example.workops.admin.user.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

/**
 * non-local profileでCognito {@code AdminCreateUser} を呼び出すユーザー作成境界。
 *
 * <p>招待メール送信はCognito標準の一時パスワードフローに任せるため、 {@code MessageAction} は指定しない。Cognito例外と {@code sub} 欠落は、
 * 画面操作へ返せる {@link ResponseStatusException} に変換する。
 */
@Service
@Profile("!local")
public class AwsCognitoUserProvisioner implements CognitoUserProvisioner {

  private static final String ATTRIBUTE_EMAIL = "email";
  private static final String ATTRIBUTE_EMAIL_VERIFIED = "email_verified";
  private static final String ATTRIBUTE_SUB = "sub";
  private static final String ATTRIBUTE_TRUE = "true";

  private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
  private final String userPoolId;

  public AwsCognitoUserProvisioner(
      CognitoIdentityProviderClient cognitoIdentityProviderClient,
      @Value("${WORKOPS_COGNITO_USER_POOL_ID:}") String userPoolId) {
    this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
    this.userPoolId = userPoolId;
  }

  /**
   * Cognito {@code AdminCreateUser} を呼び出し、レスポンス属性の {@code sub} を返す。
   *
   * @param request Cognitoユーザー作成に必要な最小入力
   * @return Cognitoが発行した {@code sub}
   * @throws ResponseStatusException username重複、Cognito API失敗、または {@code sub} 欠落の場合
   */
  @Override
  public ProvisionedCognitoUser provision(CognitoUserProvisionRequest request) {
    try {
      AdminCreateUserResponse response =
          cognitoIdentityProviderClient.adminCreateUser(adminCreateUserRequest(request));
      return new ProvisionedCognitoUser(extractCognitoSub(request, response));
    } catch (UsernameExistsException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cognitoユーザー名は既に使用されています。", exception);
    } catch (CognitoIdentityProviderException exception) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Cognitoユーザー作成に失敗しました。", exception);
    }
  }

  private AdminCreateUserRequest adminCreateUserRequest(CognitoUserProvisionRequest request) {
    return AdminCreateUserRequest.builder()
        .userPoolId(userPoolId)
        .username(request.username())
        .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
        .userAttributes(
            AttributeType.builder().name(ATTRIBUTE_EMAIL).value(request.email()).build(),
            AttributeType.builder().name(ATTRIBUTE_EMAIL_VERIFIED).value(ATTRIBUTE_TRUE).build())
        .build();
  }

  private String extractCognitoSub(
      CognitoUserProvisionRequest request, AdminCreateUserResponse response) {
    return Optional.ofNullable(response.user())
        .flatMap(
            user ->
                user.attributes().stream()
                    .filter(attribute -> ATTRIBUTE_SUB.equals(attribute.name()))
                    .map(AttributeType::value)
                    .findFirst())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Cognitoユーザー作成結果にsubが含まれていません。"));
  }
}
