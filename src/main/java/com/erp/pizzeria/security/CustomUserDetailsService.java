package com.erp.pizzeria.security;

import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.UsuarioRepository;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final LoginAttemptService attemptService;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository, LoginAttemptService attemptService) {
        this.usuarioRepository = usuarioRepository;
        this.attemptService = attemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (attemptService.estaBloqueado(username)) {
            throw new LockedException("Cuenta bloqueada temporalmente por demasiados intentos fallidos.");
        }
        Usuario usuario = usuarioRepository.findByUsernameConRelaciones(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String authority = "ROLE_" + usuario.getRol().getNombre().toUpperCase(Locale.ROOT);

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .disabled(!Boolean.TRUE.equals(usuario.getEstado()))
                .authorities(new SimpleGrantedAuthority(authority))
                .build();
    }
}
