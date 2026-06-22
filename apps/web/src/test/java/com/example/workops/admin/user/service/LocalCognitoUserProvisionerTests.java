package com.example.workops.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalCognitoUserProvisionerTests {

  private final LocalCognitoUserProvisioner provisioner = new LocalCognitoUserProvisioner();

  @Test
  void provisionReturnsUuidFormattedCognitoSub() {
    ProvisionedCognitoUser result = provisioner.provision(request());

    assertThat(result.cognitoSub()).hasSize(36);
    assertThatCode(() -> UUID.fromString(result.cognitoSub())).doesNotThrowAnyException();
  }

  @Test
  void provisionReturnsDifferentCognitoSubOnEachCall() {
    ProvisionedCognitoUser first = provisioner.provision(request());
    ProvisionedCognitoUser second = provisioner.provision(request());

    assertThat(first.cognitoSub()).isNotEqualTo(second.cognitoSub());
  }

  private CognitoUserProvisionRequest request() {
    return new CognitoUserProvisionRequest("new-user", "new-user@example.local");
  }
}
