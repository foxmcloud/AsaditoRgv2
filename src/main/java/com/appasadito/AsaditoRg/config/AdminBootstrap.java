package com.appasadito.AsaditoRg.config;

import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Optional initial staff accounts; existing roles and passwords are never overwritten. */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UsuarioRepository users;
    private final PasswordEncoder encoder;
    private final Validator validator;
    @Value("${asadito.admin.email:}") private String email;
    @Value("${asadito.admin.password:}") private String password;
    @Value("${asadito.operador.email:}") private String operatorEmail;
    @Value("${asadito.operador.password:}") private String operatorPassword;
    public AdminBootstrap(UsuarioRepository users, PasswordEncoder encoder, Validator validator) {
        this.users = users; this.encoder = encoder; this.validator = validator;
    }
    @Override
    public void run(String... args) {
        create(email, password, "ADMIN");
        create(operatorEmail, operatorPassword, "OPERADOR");
    }
    private void create(String email, String password, String role) {
        if (email.isBlank() && password.isBlank()) return;
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!validator.validate(new Account(normalized, password)).isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("Configurá ASADITO_" + role + "_EMAIL y ASADITO_" + role
                + "_PASSWORD sin comillas (email válido, contraseña de 10 caracteres mínimo y 72 bytes máximo)");
        if (users.existsByEmail(normalized)) {
            if (!users.findByEmail(normalized).orElseThrow().getRol().equals(role))
                throw new IllegalStateException("El email de bootstrap ya pertenece a un usuario sin rol " + role);
        } else users.saveAndFlush(new Usuario(normalized, encoder.encode(password), role));
    }
    private static class Account {
        @NotBlank @Email @Size(max = 120) private final String email;
        @NotBlank @Size(min = 10, max = 72) private final String password;
        private Account(String email, String password) { this.email = email; this.password = password; }
    }
}
