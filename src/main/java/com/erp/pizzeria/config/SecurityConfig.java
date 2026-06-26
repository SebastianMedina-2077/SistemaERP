package com.erp.pizzeria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler successHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/cajero/**").hasRole("CAJERO")
                        .requestMatchers("/cocina/**").hasRole("COCINA")
                        // Crear una venta es exclusivo del cajero; cocina solo lee el tablero y mueve estados.
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CAJERO")
                        .requestMatchers("/api/pedidos/**").hasAnyRole("CAJERO", "COCINA")
                        .requestMatchers("/api/productos/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/stock/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        // Stream SSE en tiempo real: lo consumen las tres pantallas.
                        .requestMatchers("/api/eventos").hasAnyRole("ADMINISTRADOR", "CAJERO", "COCINA")
                        // Verificacion de la propia contrasena: basta con estar autenticado.
                        .requestMatchers("/api/account/**").authenticated()
                        .anyRequest().authenticated())
                // Las rutas /api/** las consume JavaScript: si la sesion expira responde 401
                // (JSON) en vez de redirigir al HTML del login, que romperia los fetch.
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        // Distingue cuenta bloqueada por intentos de credenciales incorrectas.
                        .failureHandler((request, response, exception) -> {
                            String destino = (exception instanceof LockedException) ? "/login?locked" : "/login?error";
                            response.sendRedirect(request.getContextPath() + destino);
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            String target = "/login";
            if (roles.contains("ROLE_ADMINISTRADOR")) {
                target = "/admin/dashboard";
            } else if (roles.contains("ROLE_CAJERO")) {
                target = "/cajero/pos";
            } else if (roles.contains("ROLE_COCINA")) {
                target = "/cocina";
            }
            response.sendRedirect(request.getContextPath() + target);
        };
    }
}
