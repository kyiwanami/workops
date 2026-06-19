package com.example.workops.common.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.example.workops.common.logging.SecurityEventLogger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SecurityAuthorizationEventListenerTests {

    @Test
    void authorizationDeniedEventOutputsLoginUserContext(CapturedOutput output) {
        SecurityAuthorizationEventListener listener = new SecurityAuthorizationEventListener(
                new SecurityEventLogger());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginUserContext(),
                null,
                List.of(new SimpleGrantedAuthority("TENANT_MANAGER")));

        listener.onAuthorizationDenied(new AuthorizationDeniedEvent<>(
                () -> authentication,
                new Object(),
                new AuthorizationDecision(false)));

        String logs = output.getAll();
        assertThat(logs).contains(
                "userId=10 companyId=1 actorType=TENANT authorities=TENANT_MANAGER "
                        + "eventType=AUTHORIZATION_DENIED result=REJECTED "
                        + "reasonCode=AUTHORIZATION_DENIED exceptionType=AccessDeniedException");
        assertThat(logs).doesNotContain("tenant-user", "tenant-user@example.local");
    }

    @Test
    void authorizationDeniedEventWithoutLoginUserContextOutputsHyphen(CapturedOutput output) {
        SecurityAuthorizationEventListener listener = new SecurityAuthorizationEventListener(
                new SecurityEventLogger());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "principal-name",
                null,
                List.of(new SimpleGrantedAuthority("TENANT_MANAGER")));

        listener.onAuthorizationDenied(new AuthorizationDeniedEvent<>(
                () -> authentication,
                new Object(),
                new AuthorizationDecision(false)));

        String logs = output.getAll();
        assertThat(logs).contains(
                "userId=- companyId=- actorType=- authorities=- eventType=AUTHORIZATION_DENIED "
                        + "result=REJECTED reasonCode=AUTHORIZATION_DENIED "
                        + "exceptionType=AccessDeniedException");
        assertThat(logs).doesNotContain("principal-name");
    }

    private LoginUserContext loginUserContext() {
        return new LoginUserContext(
                10L,
                "tenant-user",
                "tenant-user@example.local",
                "TENANT",
                1L,
                List.of(new PermissionSetContext("TENANT_MANAGER", "管理者")));
    }
}
