package compass.career.evaluationsapi.configuration;

import compass.career.evaluationsapi.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header de Authorization, continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extraer el token JWT
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            // Si el token es válido y no hay autenticación previa
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validar el token
                if (jwtService.isTokenValid(jwt)) {

                    // ✅ SOLUCIÓN: Extraer rol y validar que NO sea null
                    String userRole = jwtService.extractClaim(jwt, claims -> claims.get("role", String.class));

                    // ✅ Si el rol es null, asignar un rol por defecto o rechazar
                    if (userRole == null || userRole.isEmpty()) {
                        logger.warn("Token válido pero sin rol definido para usuario: " + userEmail);
                        userRole = "UNIVERSITY_STUDENT"; // Rol por defecto
                        // O puedes rechazar la petición:
                        // response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        // return;
                    }

                    // Crear autoridad con validación
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                            "ROLE_" + userRole.toUpperCase()
                    );

                    // Crear token de autenticación
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            Collections.singletonList(authority)
                    );

                    // Establecer autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.debug("Usuario autenticado: " + userEmail + " con rol: " + userRole);
                }
            }
        } catch (Exception e) {
            logger.error("Error procesando JWT: " + e.getMessage());
            // No propagar la excepción, solo logearla
        }

        filterChain.doFilter(request, response);
    }
}