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

    private static final String ACTOR_TYPE_PLATFORM = "PLATFORM";
    private static final String ACTOR_TYPE_TENANT = "TENANT";

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
        assertValidLoginUser(row, permissionSets);

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

    private void assertValidLoginUser(UserAccountRow row, List<PermissionSetContext> permissionSets) {
        if (permissionSets.isEmpty()) {
            throw new IllegalStateException("ログインユーザーに権限セットが割り当てられていません。");
        }

        List<String> permissionSetCodes = permissionSets.stream()
                .map(PermissionSetContext::code)
                .toList();

        if (ACTOR_TYPE_PLATFORM.equals(row.actorType())) {
            if (row.companyId() != null || !PermissionSetCode.isValidPlatformCodes(permissionSetCodes)) {
                throw new IllegalStateException("PLATFORMユーザーの権限セットまたは会社設定が不正です。");
            }
            return;
        }

        if (ACTOR_TYPE_TENANT.equals(row.actorType())) {
            if (row.companyId() == null || !PermissionSetCode.isValidTenantCodes(permissionSetCodes)) {
                throw new IllegalStateException("TENANTユーザーの権限セットまたは会社設定が不正です。");
            }
            return;
        }

        throw new IllegalStateException("actor_typeが不正です。");
    }
}
