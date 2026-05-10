package com.example.workops.common.security;

/**
 * Spring Securityから取得した認証主体をWorkOps内部へ渡す最小コンテキスト。
 */
public record LoginUserContext(
        String providerSubject) {
}
