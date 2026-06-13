package com.erp.pizzeria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

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
                        .requestMatchers("/api/pedidos/**").hasAnyRole("CAJERO", "COCINA")
                        .requestMatchers("/api/productos/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/stock/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
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
