package com.appasadito.AsaditoRg.config;

import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Optional first administrator. No default password and no public role assignment. */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UsuarioRepository users;
    private final PasswordEncoder encoder;
    @Value("${asadito.admin.email:}") private String email;
    @Value("${asadito.admin.password:}") private String password;
    public AdminBootstrap(UsuarioRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }
    @Override
    public void run(String... args) {
        if (email.isBlank() && password.isBlank()) return;
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 120 || password.length() < 10
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("Configurá ASADITO_ADMIN_EMAIL y ASADITO_ADMIN_PASSWORD (10 caracteres mínimo, 72 bytes máximo)");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalized)) {
            if (!users.findByEmail(normalized).orElseThrow().getRol().equals("ADMIN"))
                throw new IllegalStateException("El email de bootstrap ya pertenece a un usuario sin rol ADMIN");
        } else users.save(new Usuario(normalized, encoder.encode(password), "ADMIN"));
    }
}
