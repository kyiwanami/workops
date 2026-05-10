package com.example.workops.common.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(MethodSecurityPreAuthorizeTests.MethodSecurityTestConfig.class)
class MethodSecurityPreAuthorizeTests {

    @Autowired
    private SecuredOperation securedOperation;

    @Test
    void managerCanInvokePreAuthorizedMethod() {
        LoginUserContext manager = loginUserContext(List.of(new PermissionSetContext("TENANT_MANAGER", "管理者")));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                manager,
                null,
                List.of(new SimpleGrantedAuthority("TENANT_MANAGER"))));

        assertThat(securedOperation.managerOnly()).isEqualTo("allowed");
        SecurityContextHolder.clearContext();
    }

    @Test
    void viewerCannotInvokePreAuthorizedMethod() {
        LoginUserContext viewer = loginUserContext(List.of(new PermissionSetContext("TENANT_VIEWER", "閲覧者")));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                viewer,
                null,
                List.of(new SimpleGrantedAuthority("TENANT_VIEWER"))));

        assertThatThrownBy(() -> securedOperation.managerOnly())
                .isInstanceOf(AccessDeniedException.class);
        SecurityContextHolder.clearContext();
    }

    private LoginUserContext loginUserContext(List<PermissionSetContext> permissionSets) {
        return new LoginUserContext(
                1L,
                "kthm-user",
                "kthm-user@example.local",
                "TENANT",
                1L,
                permissionSets);
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {

        @Bean
        SecuredOperation securedOperation() {
            return new SecuredOperation();
        }
    }

    /**
     * method securityがWorkOpsAuthorizationを呼び出せることを確認するテスト専用Service。
     */
    static class SecuredOperation {

        @PreAuthorize("hasAuthority('TENANT_MANAGER')")
        String managerOnly() {
            return "allowed";
        }
    }
}
