package com.example.workops.admin.user.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * non-local profileでCognito Identity Provider clientを構成する。
 *
 * <p>RegionはSpring設定として読み込んだ {@code AWS_REGION} を明示的に使い、 認証情報はAWS SDKのDefault Credentials Provider
 * Chainに任せる。
 */
@Configuration
@Profile("!local")
public class CognitoIdentityProviderClientConfig {

  /**
   * Cognito {@code AdminCreateUser} 呼び出しに使うAWS SDK clientを作成する。
   *
   * @param region AWS region
   * @return Cognito Identity Provider client
   */
  @Bean
  CognitoIdentityProviderClient cognitoIdentityProviderClient(
      @Value("${AWS_REGION}") String region) {
    return CognitoIdentityProviderClient.builder()
        .region(Region.of(region))
        .overrideConfiguration(
            ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(5))
                .apiCallTimeout(Duration.ofSeconds(20))
                .build())
        .build();
  }
}
