package com.esprit.microservice.pidev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final OAuth2SuccessHandler oauth2SuccessHandler;

    public SecurityConfig(OAuth2SuccessHandler oauth2SuccessHandler) {
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // ── Swagger ──────────────────────────────────────
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()

                        // ── Auth + OAuth2 endpoints ───────────────────────
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",               // Google redirect trigger
                                "/login/oauth2/**"           // Google callback — THIS was causing 403
                        ).permitAll()

                        // ── Public modules ────────────────────────────────
                        .requestMatchers(
                                "/api/subscriptions/**",
                                "/api/user-subscriptions/**",
                                "/api/users/**",
                                "/users/**",
                                "/api/evenements/**",
                                "/api/forum/**"
                        ).permitAll()

                        // ── Everything else requires auth ─────────────────
                        .anyRequest().authenticated()
                )

                // ── OAuth2 Login ──────────────────────────────────────
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler)   // posts token back to Angular popup
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(401, "Unauthorized");
                        })
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }


}