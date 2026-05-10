package com.example.workops.common.security;

/**
 * WorkOps内部で扱うログイン済みユーザーの最小コンテキスト。
 */
public record LoginUserContext(
        String providerSubject,
        String username,
        String email) {
}
