package com.example.workops.admin.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * non-local profileでCognito Identity Provider clientを構成する。
 */
@Configuration
@Profile("!local")
public class CognitoIdentityProviderClientConfig {

    @Bean
    CognitoIdentityProviderClient cognitoIdentityProviderClient(
            @Value("${AWS_REGION}") String region) {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }
}
