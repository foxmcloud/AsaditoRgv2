package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioRepository users;
    private final PasswordEncoder encoder;
    public AuthController(UsuarioRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }
    public static class Registro {
        @NotBlank @Email @Size(max = 120) public String email;
        public String getEmail() { return email; }
        @NotBlank @Size(min = 10, max = 72) public String password;
        public String getPassword() { return password; }
    }
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        boolean loggedIn = auth != null && auth.isAuthenticated();
        return Map.of("authenticated", loggedIn, "email", loggedIn ? auth.getName() : "",
            "admin", loggedIn && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody Registro request) {
        if (request.password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes UTF-8");
        String email = request.email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo registrar ese email");
        users.saveAndFlush(new Usuario(email, encoder.encode(request.password), "USER"));
        return Map.of("message", "Cuenta creada. Ya podés iniciar sesión.");
    }
}
