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

import java.util.Arrays;
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
                        // Endpoints públicos
                        .requestMatchers(
                                "/api/v1/evaluations/personality-test",
                                "/api/v1/evaluations/vocational-interests-test",
                                "/api/v1/evaluations/cognitive-skills-test",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Careers - Endpoints con acceso mixto
                        .requestMatchers("GET", "/api/v1/careers").authenticated()
                        .requestMatchers("GET", "/api/v1/careers/{careerId}").authenticated()
                        .requestMatchers("GET", "/api/v1/careers/details/{careerId}").authenticated()
                        .requestMatchers("POST", "/api/v1/careers/recommendations").hasRole("UNIVERSITY_STUDENT")
                        .requestMatchers("POST", "/api/v1/careers").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/v1/careers/**").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/v1/careers/**").hasRole("ADMIN")

                        // Specializations - Solo UNIVERSITY_STUDENT
                        .requestMatchers("/api/v1/specializations/**").hasRole("UNIVERSITY_STUDENT")

                        // Evaluations - Solo UNIVERSITY_STUDENT
                        .requestMatchers("/api/v1/evaluations/**").hasRole("UNIVERSITY_STUDENT")

                        // Favorite Careers - Solo UNIVERSITY_STUDENT
                        .requestMatchers("/api/v1/favorite-careers/**").hasRole("UNIVERSITY_STUDENT")

                        // Favorite Specializations - Solo UNIVERSITY_STUDENT
                        .requestMatchers("/api/v1/favorite-specializations/**").hasRole("UNIVERSITY_STUDENT")

                        // Tests (Admin) - Solo ADMIN
                        .requestMatchers("/api/v1/tests/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required to access this resource\",\"path\":\"%s\"}",
                                    java.time.Instant.now(),
                                    request.getRequestURI()
                            ));
                        })

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

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Authorization"
        ));

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}