package com.example.workops.common.security;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.workops.master.mapper.UserAccountMapper;
import com.example.workops.master.mapper.UserAccountRow;
import com.example.workops.master.mapper.PermissionSetRow;

/**
 * 認証主体のsubからDB由来のWorkOps利用者コンテキストを作る。
 */
@Component
public class DbLoginUserContextFactory {

    private final UserAccountMapper userAccountMapper;

    public DbLoginUserContextFactory(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    public Optional<LoginUserContext> fromCognitoSub(String cognitoSub) {
        if (cognitoSub == null || cognitoSub.isBlank()) {
            return Optional.empty();
        }

        return userAccountMapper.findByCognitoSub(cognitoSub)
                .map(this::toLoginUserContext);
    }

    private LoginUserContext toLoginUserContext(UserAccountRow row) {
        List<PermissionSetContext> permissionSets = userAccountMapper.findPermissionSetsByUserId(row.userId())
                .stream()
                .map(this::toPermissionSetContext)
                .toList();

        return new LoginUserContext(
                row.userId(),
                row.username(),
                row.email(),
                row.actorType(),
                row.companyId(),
                permissionSets);
    }

    private PermissionSetContext toPermissionSetContext(PermissionSetRow row) {
        return new PermissionSetContext(row.code(), row.name());
    }
}
