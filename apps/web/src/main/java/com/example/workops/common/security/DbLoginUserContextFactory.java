package com.example.workops.common.security;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.workops.master.mapper.UserAccountMapper;
import com.example.workops.master.mapper.UserAccountRow;
import com.example.workops.master.mapper.PermissionSetRow;
import com.example.workops.common.logging.SecurityEventLogger;
import com.example.workops.common.logging.SecurityEventLogRecord;

/**
 * 認証主体のsubからDB由来のWorkOps利用者コンテキストを作る。
 */
@Component
public class DbLoginUserContextFactory {

    private static final String ACTOR_TYPE_PLATFORM = "PLATFORM";
    private static final String ACTOR_TYPE_TENANT = "TENANT";
    private static final String EVENT_TYPE_AUTHENTICATION_REJECTED = "AUTHENTICATION_REJECTED";
    private static final String REASON_CODE_PERMISSION_SET_NOT_ASSIGNED = "PERMISSION_SET_NOT_ASSIGNED";
    private static final String REASON_CODE_INVALID_PERMISSION_SET = "INVALID_PERMISSION_SET";
    private static final String REASON_CODE_INVALID_ACTOR_TYPE = "INVALID_ACTOR_TYPE";

    private final UserAccountMapper userAccountMapper;
    private final SecurityEventLogger securityEventLogger;

    public DbLoginUserContextFactory(UserAccountMapper userAccountMapper, SecurityEventLogger securityEventLogger) {
        this.userAccountMapper = userAccountMapper;
        this.securityEventLogger = securityEventLogger;
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
            logInvalidLoginUser(row, permissionSets, REASON_CODE_PERMISSION_SET_NOT_ASSIGNED);
            throw new IllegalStateException("ログインユーザーに権限セットが割り当てられていません。");
        }

        List<String> permissionSetCodes = permissionSets.stream()
                .map(PermissionSetContext::code)
                .toList();

        if (ACTOR_TYPE_PLATFORM.equals(row.actorType())) {
            if (row.companyId() != null || !PermissionSetCode.isValidPlatformCodes(permissionSetCodes)) {
                logInvalidLoginUser(row, permissionSets, REASON_CODE_INVALID_PERMISSION_SET);
                throw new IllegalStateException("PLATFORMユーザーの権限セットまたは会社設定が不正です。");
            }
            return;
        }

        if (ACTOR_TYPE_TENANT.equals(row.actorType())) {
            if (row.companyId() == null || !PermissionSetCode.isValidTenantCodes(permissionSetCodes)) {
                logInvalidLoginUser(row, permissionSets, REASON_CODE_INVALID_PERMISSION_SET);
                throw new IllegalStateException("TENANTユーザーの権限セットまたは会社設定が不正です。");
            }
            return;
        }

        logInvalidLoginUser(row, permissionSets, REASON_CODE_INVALID_ACTOR_TYPE);
        throw new IllegalStateException("actor_typeが不正です。");
    }

    private void logInvalidLoginUser(
            UserAccountRow row,
            List<PermissionSetContext> permissionSets,
            String reasonCode) {
        LoginUserContext loginUserContext = new LoginUserContext(
                row.userId(),
                row.username(),
                row.email(),
                row.actorType(),
                row.companyId(),
                permissionSets);
        securityEventLogger.logRejected(new SecurityEventLogRecord(
                loginUserContext,
                EVENT_TYPE_AUTHENTICATION_REJECTED,
                reasonCode,
                IllegalStateException.class.getSimpleName()));
    }
}
