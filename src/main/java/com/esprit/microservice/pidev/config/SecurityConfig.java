package com.esprit.microservice.pidev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Swagger
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/api/projects/**",
                                "/api/skills/**",
                                "/api/applications/**",
                                "/api/applications/cover-letter/**"
                        ).permitAll()

                        // Auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        // DOIT être en permitAll sinon Spring Security bloque le PDF
                        .requestMatchers("/uploads/**").permitAll()
                        // Public modules
                        .requestMatchers(
                                "/api/subscriptions/**",
                                "/api/user-subscriptions/**",
                                "/api/users/**",
                                "/api/evenements/**",
                                "/api/forum/**",
                                "/api/projects/**",
                                "/api/skills/**",
                                "/api/applications/**",
                                "/api/applications/cover-letter/**"
                        ).permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}
