package com.example.workops.admin.user.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AwsCognitoUserProvisionerTests {

    private static final String USER_POOL_ID = "ap-northeast-1_example";
    private static final String USERNAME = "new-user";
    private static final String EMAIL = "new-user@example.local";
    private static final String COGNITO_SUB = "11111111-2222-3333-4444-555555555555";

    private CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private AwsCognitoUserProvisioner provisioner;

    @BeforeEach
    void setUp() {
        cognitoIdentityProviderClient = mock(CognitoIdentityProviderClient.class);
        provisioner = new AwsCognitoUserProvisioner(cognitoIdentityProviderClient, USER_POOL_ID);
    }

    @Test
    void provisionCallsAdminCreateUserAndReturnsCognitoSub() {
        when(cognitoIdentityProviderClient.adminCreateUser(org.mockito.ArgumentMatchers.any(AdminCreateUserRequest.class)))
                .thenReturn(responseWithSub());

        ProvisionedCognitoUser result = provisioner.provision(request());

        assertThat(result.cognitoSub()).isEqualTo(COGNITO_SUB);
        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(cognitoIdentityProviderClient).adminCreateUser(captor.capture());
        AdminCreateUserRequest actualRequest = captor.getValue();
        assertThat(actualRequest.userPoolId()).isEqualTo(USER_POOL_ID);
        assertThat(actualRequest.username()).isEqualTo(USERNAME);
        assertThat(actualRequest.desiredDeliveryMediums()).containsExactly(DeliveryMediumType.EMAIL);
        assertThat(attributeValue(actualRequest.userAttributes(), "email")).isEqualTo(EMAIL);
        assertThat(attributeValue(actualRequest.userAttributes(), "email_verified")).isEqualTo("true");
        assertThat(actualRequest.messageActionAsString()).isNull();
    }

    @Test
    void usernameExistsExceptionIsConverted() {
        when(cognitoIdentityProviderClient.adminCreateUser(org.mockito.ArgumentMatchers.any(AdminCreateUserRequest.class)))
                .thenThrow(UsernameExistsException.builder().message("exists").build());

        assertThatThrownBy(() -> provisioner.provision(request()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void cognitoExceptionIsConverted() {
        when(cognitoIdentityProviderClient.adminCreateUser(org.mockito.ArgumentMatchers.any(AdminCreateUserRequest.class)))
                .thenThrow(CognitoIdentityProviderException.builder().message("failed").build());

        assertThatThrownBy(() -> provisioner.provision(request()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void missingSubIsConvertedToProvisionException() {
        AdminCreateUserResponse response = AdminCreateUserResponse.builder()
                .user(UserType.builder().attributes(List.of()).build())
                .build();
        when(cognitoIdentityProviderClient.adminCreateUser(org.mockito.ArgumentMatchers.any(AdminCreateUserRequest.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> provisioner.provision(request()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private CognitoUserProvisionRequest request() {
        return new CognitoUserProvisionRequest(USERNAME, EMAIL);
    }

    private AdminCreateUserResponse responseWithSub() {
        return AdminCreateUserResponse.builder()
                .user(UserType.builder()
                        .attributes(AttributeType.builder()
                                .name("sub")
                                .value(COGNITO_SUB)
                                .build())
                        .build())
                .build();
    }

    private String attributeValue(List<AttributeType> attributes, String name) {
        return attributes.stream()
                .filter(attribute -> name.equals(attribute.name()))
                .map(AttributeType::value)
                .findFirst()
                .orElse(null);
    }
}
