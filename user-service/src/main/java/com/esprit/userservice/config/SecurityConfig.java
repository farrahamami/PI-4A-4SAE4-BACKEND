package com.esprit.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Bean;

@Configuration
public class SecurityConfig {

    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(OAuth2SuccessHandler oauth2SuccessHandler,
                          JwtAuthFilter jwtAuthFilter) {
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS désactivé ici — géré par l'API Gateway (application.yml globalcors)
                // Ne pas configurer CORS dans le microservice, sinon double header → erreur browser
                .cors(cors -> cors.disable())

                // ❌ Disable CSRF (APIs stateless)
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // ✅ Autoriser les requêtes preflight OPTIONS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()

                        // Auth endpoints — publics, pas besoin de JWT
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/api/ai/**"
                        ).permitAll()

                        // APIs publiques
                        .requestMatchers(
                                "/api/subscriptions/**",
                                "/api/user-subscriptions/**",
                                "/api/users/**",
                                "/users/**",
                                "/api/evenements/**",
                                "/api/forum/**",
                                "/api/courses/**"
                        ).permitAll()

                        // Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                )

                // ✅ Filtre JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // OAuth2 login
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler)
                )

                // Erreurs d'authentification
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