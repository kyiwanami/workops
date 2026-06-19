package com.example.workops.common.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.example.workops.common.logging.SecurityEventLogger;
import com.example.workops.master.mapper.PermissionSetRow;
import com.example.workops.master.mapper.UserAccountMapper;
import com.example.workops.master.mapper.UserAccountRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class DbLoginUserContextFactoryTests {

    private static final String COGNITO_SUB = "11111111-2222-3333-4444-555555555555";

    @Test
    void permissionSetNotAssignedOutputsSecurityEvent(CapturedOutput output) {
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        when(userAccountMapper.findByCognitoSub(COGNITO_SUB))
                .thenReturn(java.util.Optional.of(tenantUser()));
        when(userAccountMapper.findPermissionSetsByUserId(10L))
                .thenReturn(List.of());
        DbLoginUserContextFactory factory = new DbLoginUserContextFactory(
                userAccountMapper,
                new SecurityEventLogger());

        assertThatThrownBy(() -> factory.fromCognitoSub(COGNITO_SUB))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ログインユーザーに権限セットが割り当てられていません。");

        assertThat(output.getAll()).contains(
                "userId=10 companyId=1 actorType=TENANT authorities=- "
                        + "eventType=AUTHENTICATION_REJECTED result=REJECTED "
                        + "reasonCode=PERMISSION_SET_NOT_ASSIGNED exceptionType=IllegalStateException");
    }

    @Test
    void invalidPermissionSetOutputsSecurityEvent(CapturedOutput output) {
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        when(userAccountMapper.findByCognitoSub(COGNITO_SUB))
                .thenReturn(java.util.Optional.of(tenantUser()));
        when(userAccountMapper.findPermissionSetsByUserId(10L))
                .thenReturn(List.of(permission("PLATFORM_ADMIN", "WorkOps管理者")));
        DbLoginUserContextFactory factory = new DbLoginUserContextFactory(
                userAccountMapper,
                new SecurityEventLogger());

        assertThatThrownBy(() -> factory.fromCognitoSub(COGNITO_SUB))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANTユーザーの権限セットまたは会社設定が不正です。");

        assertThat(output.getAll()).contains(
                "userId=10 companyId=1 actorType=TENANT authorities=PLATFORM_ADMIN "
                        + "eventType=AUTHENTICATION_REJECTED result=REJECTED "
                        + "reasonCode=INVALID_PERMISSION_SET exceptionType=IllegalStateException");
        assertThat(output.getAll()).doesNotContain("tenant-user", "tenant-user@example.local", COGNITO_SUB);
    }

    @Test
    void invalidActorTypeOutputsSecurityEvent(CapturedOutput output) {
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        when(userAccountMapper.findByCognitoSub(COGNITO_SUB))
                .thenReturn(java.util.Optional.of(new UserAccountRow(
                        10L,
                        "tenant-user",
                        "tenant-user@example.local",
                        "UNKNOWN",
                        1L)));
        when(userAccountMapper.findPermissionSetsByUserId(10L))
                .thenReturn(List.of(permission("TENANT_MANAGER", "管理者")));
        DbLoginUserContextFactory factory = new DbLoginUserContextFactory(
                userAccountMapper,
                new SecurityEventLogger());

        assertThatThrownBy(() -> factory.fromCognitoSub(COGNITO_SUB))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("actor_typeが不正です。");

        assertThat(output.getAll()).contains(
                "userId=10 companyId=1 actorType=UNKNOWN authorities=TENANT_MANAGER "
                        + "eventType=AUTHENTICATION_REJECTED result=REJECTED "
                        + "reasonCode=INVALID_ACTOR_TYPE exceptionType=IllegalStateException");
    }

    private UserAccountRow tenantUser() {
        return new UserAccountRow(
                10L,
                "tenant-user",
                "tenant-user@example.local",
                "TENANT",
                1L);
    }

    private PermissionSetRow permission(String code, String name) {
        return new PermissionSetRow(code, name);
    }
}
