package com.erp.pizzeria.security;

import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.UsuarioRepository;
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

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
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
