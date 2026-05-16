package com.example.workops.admin.user.service;

/**
 * WorkOpsのユーザー作成からCognitoユーザー作成を呼び出す境界。
 */
public interface CognitoUserProvisioner {

    ProvisionedCognitoUser provision(CognitoUserProvisionRequest request);
}
