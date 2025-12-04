package compass.career.evaluationsapi.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // ENDPOINTS PÚBLICOS (sin autenticación)
                        // ========================================
                        .requestMatchers(
                                // Tests de evaluación (públicos para que cualquiera pueda verlos)
                                "/api/v1/evaluations/personality-test",
                                "/api/v1/evaluations/vocational-interests-test",
                                "/api/v1/evaluations/cognitive-skills-test",
                                // Actuator (health checks para Eureka)
                                "/actuator/**",
                                // Swagger/OpenAPI
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ========================================
                        // CAREERS - Endpoints con acceso mixto
                        // ========================================
                        // GET /api/v1/careers - Todos los roles autenticados
                        .requestMatchers("GET", "/api/v1/careers").authenticated()

                        // GET /api/v1/careers/{id} - Todos los roles autenticados
                        .requestMatchers("GET", "/api/v1/careers/{careerId}").authenticated()

                        // GET /api/v1/careers/details/{id} - Todos los roles autenticados
                        .requestMatchers("GET", "/api/v1/careers/details/{careerId}").authenticated()

                        // POST /api/v1/careers/recommendations - Solo UNIVERSITY_STUDENT
                        .requestMatchers("POST", "/api/v1/careers/recommendations").hasRole("UNIVERSITY_STUDENT")

                        // POST /api/v1/careers (crear) - Solo ADMIN
                        .requestMatchers("POST", "/api/v1/careers").hasRole("ADMIN")

                        // PUT /api/v1/careers/{id} (actualizar) - Solo ADMIN
                        .requestMatchers("PUT", "/api/v1/careers/**").hasRole("ADMIN")

                        // DELETE /api/v1/careers/{id} (eliminar) - Solo ADMIN
                        .requestMatchers("DELETE", "/api/v1/careers/**").hasRole("ADMIN")

                        // ========================================
                        // SPECIALIZATIONS - Solo UNIVERSITY_STUDENT
                        // ========================================
                        .requestMatchers("/api/v1/specializations/**").hasRole("UNIVERSITY_STUDENT")

                        // ========================================
                        // EVALUATIONS - Solo UNIVERSITY_STUDENT
                        // ========================================
                        .requestMatchers("/api/v1/evaluations/**").hasRole("UNIVERSITY_STUDENT")

                        // ========================================
                        // FAVORITE CAREERS - Solo UNIVERSITY_STUDENT
                        // ========================================
                        .requestMatchers("/api/v1/favorite-careers/**").hasRole("UNIVERSITY_STUDENT")

                        // ========================================
                        // FAVORITE SPECIALIZATIONS - Solo UNIVERSITY_STUDENT
                        // ========================================
                        .requestMatchers("/api/v1/favorite-specializations/**").hasRole("UNIVERSITY_STUDENT")

                        // ========================================
                        // TESTS (Admin) - Solo ADMIN
                        // ========================================
                        .requestMatchers("/api/v1/tests/**").hasRole("ADMIN")

                        // ========================================
                        // TODOS LOS DEMÁS: Autenticado
                        // ========================================
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ========================================
                // MANEJO DE EXCEPCIONES
                // ========================================
                .exceptionHandling(exceptions -> exceptions
                        // 401 - No autenticado (sin token o token inválido)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required to access this resource\",\"path\":\"%s\"}",
                                    java.time.Instant.now(),
                                    request.getRequestURI()
                            ));
                        })

                        // 403 - Autenticado pero sin permisos (rol incorrecto)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"You don't have permission to access this resource\",\"path\":\"%s\"}",
                                    java.time.Instant.now(),
                                    request.getRequestURI()
                            ));
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}