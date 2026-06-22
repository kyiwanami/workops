package com.example.workops.master.mapper;

/** users と権限セットをまとめて取得するためのMyBatis行オブジェクト。 */
public record UserAccountRow(
    Long userId, String username, String email, String actorType, Long companyId) {}
