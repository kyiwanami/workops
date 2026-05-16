package com.example.workops.admin.user.service;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * local profileでCognitoユーザー作成を代替し、DB登録用の疑似subを返す。
 */
@Service
@Profile("local")
public class LocalCognitoUserProvisioner implements CognitoUserProvisioner {

    @Override
    public ProvisionedCognitoUser provision(CognitoUserProvisionRequest request) {
        return new ProvisionedCognitoUser(UUID.randomUUID().toString());
    }
}
