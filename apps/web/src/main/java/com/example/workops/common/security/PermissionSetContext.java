package com.example.workops.common.security;

/** DB由来の権限セット情報を現在ユーザーコンテキストへ渡すDTO。 */
public record PermissionSetContext(String code, String name) {}
