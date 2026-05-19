package com.example.workops.common.security;

import java.util.Collection;
import java.util.Set;

/**
 * WorkOpsのMVPで扱う権限セットコード。
 */
public enum PermissionSetCode {
    PLATFORM_ADMIN,
    TENANT_VIEWER,
    TENANT_EDITOR,
    TENANT_MANAGER;

    private static final Set<String> PLATFORM_CODES = Set.of(PLATFORM_ADMIN.name());
    private static final Set<String> TENANT_CODES = Set.of(
            TENANT_VIEWER.name(),
            TENANT_EDITOR.name(),
            TENANT_MANAGER.name());

    /**
     * PLATFORMユーザーへ割り当て可能な権限セットコードを返す。
     */
    public static Set<String> platformCodes() {
        return PLATFORM_CODES;
    }

    /**
     * TENANTユーザーへ割り当て可能な権限セットコードを返す。
     */
    public static Set<String> tenantCodes() {
        return TENANT_CODES;
    }

    /**
     * PLATFORMユーザー用の権限セットコードだけで構成されているか判定する。
     */
    public static boolean isValidPlatformCodes(Collection<String> codes) {
        return PLATFORM_CODES.containsAll(codes);
    }

    /**
     * TENANTユーザー用の権限セットコードだけで構成されているか判定する。
     */
    public static boolean isValidTenantCodes(Collection<String> codes) {
        return TENANT_CODES.containsAll(codes);
    }
}
