package com.example.workops.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * local起動では認証を通さず、それ以外の起動ではCognito OAuth2 Loginを使うSecurity設定。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Profile("!local")
    public SecurityFilterChain cognitoSecurityFilterChain(
            HttpSecurity http,
            CognitoAuthenticationSuccessHandler cognitoAuthenticationSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // ALB health checks cannot pass through Cognito login.
                        .requestMatchers("/actuator/health", "/css/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        // CloudFront is the public host; start login through a relative path.
                        .loginPage("/oauth2/authorization/cognito")
                        .successHandler(cognitoAuthenticationSuccessHandler))
                .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }

    @Bean
    @Profile("local")
    public SecurityFilterChain localSecurityFilterChain(
            HttpSecurity http,
            LocalAuthenticationFilter localAuthenticationFilter) throws Exception {
        http
                .addFilterBefore(localAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
