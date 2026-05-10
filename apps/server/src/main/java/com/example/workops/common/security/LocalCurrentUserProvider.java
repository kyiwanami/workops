package com.example.workops.common.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local profileで固定subに紐づくDBユーザーを現在ユーザーとして扱うProvider。
 */
@Component
@Profile("local")
public class LocalCurrentUserProvider implements CurrentUserProvider {

    private final DbLoginUserContextFactory dbLoginUserContextFactory;
    private final String localCognitoSub;

    public LocalCurrentUserProvider(
            DbLoginUserContextFactory dbLoginUserContextFactory,
            @Value("${WORKOPS_LOCAL_COGNITO_SUB:00000000-0000-0000-0000-000000000003}") String localCognitoSub) {
        this.dbLoginUserContextFactory = dbLoginUserContextFactory;
        this.localCognitoSub = localCognitoSub;
    }

    @Override
    public Optional<LoginUserContext> currentUser() {
        return dbLoginUserContextFactory.fromCognitoSub(localCognitoSub);
    }
}
