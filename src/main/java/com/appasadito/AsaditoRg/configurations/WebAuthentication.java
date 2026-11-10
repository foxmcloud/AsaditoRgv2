package com.appasadito.AsaditoRg.configurations;

import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.*;
import java.util.Locale;

@Configuration
public class WebAuthentication {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
            if (usuario == null) throw new UsernameNotFoundException("Usuario desconocido");
            return new User(usuario.getEmail(), usuario.getPassword(),
                AuthorityUtils.createAuthorityList(usuario.getRol()));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder encoder = (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // Solo para hashes BCrypt antiguos sin prefijo {bcrypt}; no admite contraseñas en texto plano.
        encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return encoder;
    }
}
