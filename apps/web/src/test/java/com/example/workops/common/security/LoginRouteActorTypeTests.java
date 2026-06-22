package com.example.workops.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LoginRouteActorTypeTests {

  @Test
  void platformRegistrationRequiresPlatformActorType() {
    LoginRouteActorType actorType = LoginRouteActorType.fromRegistrationId("platform");

    assertThat(actorType.actorType()).isEqualTo("PLATFORM");
    assertThat(actorType.matches("PLATFORM")).isTrue();
    assertThat(actorType.matches("TENANT")).isFalse();
  }

  @Test
  void tenantRegistrationRequiresTenantActorType() {
    LoginRouteActorType actorType = LoginRouteActorType.fromRegistrationId("tenant");

    assertThat(actorType.actorType()).isEqualTo("TENANT");
    assertThat(actorType.matches("TENANT")).isTrue();
    assertThat(actorType.matches("PLATFORM")).isFalse();
  }

  @Test
  void unsupportedRegistrationIsRejected() {
    assertThatThrownBy(() -> LoginRouteActorType.fromRegistrationId("cognito"))
        .isInstanceOf(IllegalStateException.class);
  }
}
