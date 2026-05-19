package com.example.workops.admin.user.service;

/**
 * WorkOpsのユーザー作成からCognitoユーザー作成を呼び出す境界。
 *
 * <p>local profileでは疑似 {@code cognito_sub} を返し、非local profileではCognito
 * {@code AdminCreateUser} を呼び出す。呼び出し側は戻り値の {@code cognitoSub} を
 * {@code users.cognito_sub} に登録する。</p>
 */
public interface CognitoUserProvisioner {

    /**
     * Cognitoユーザーを作成し、WorkOps DBへ保存する識別子を返す。
     *
     * @param request Cognitoユーザー作成に必要な最小入力
     * @return Cognito作成済みユーザーの識別子
     */
    ProvisionedCognitoUser provision(CognitoUserProvisionRequest request);
}
