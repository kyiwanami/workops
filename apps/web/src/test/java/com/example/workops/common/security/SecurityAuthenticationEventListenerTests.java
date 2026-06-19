package com.example.workops.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import com.example.workops.common.logging.SecurityEventLogger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SecurityAuthenticationEventListenerTests {

    @Test
    void authenticationFailureEventOutputsSecurityEvent(CapturedOutput output) {
        SecurityAuthenticationEventListener listener = new SecurityAuthenticationEventListener(
                new SecurityEventLogger());

        listener.onAuthenticationFailure(new AuthenticationFailureBadCredentialsEvent(
                new UsernamePasswordAuthenticationToken("principal-name", "raw-password"),
                new BadCredentialsException("bad credentials")));

        String logs = output.getAll();
        assertThat(logs).contains(
                "userId=- companyId=- actorType=- authorities=- eventType=AUTHENTICATION_FAILED "
                        + "result=REJECTED reasonCode=AUTHENTICATION_FAILED "
                        + "exceptionType=BadCredentialsException");
        assertThat(logs).doesNotContain("principal-name", "raw-password", "bad credentials");
    }
}
