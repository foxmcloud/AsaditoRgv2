package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UsuarioController {
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @GetMapping("/auth/me")
    public Map<String, Object> actual(Authentication authentication) {
        if (authentication == null) return Map.of("authenticated", false, "email", "", "admin", false, "operador", false);
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        return Map.of("authenticated", true, "email", usuario.getEmail(),
            "admin", usuario.getRol().equals("ADMIN"), "operador", usuario.getRol().equals("OPERADOR"));
    }
    @PostMapping("/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> registrar(@RequestParam String email, @RequestParam String password) {
        return crear(email, password, "USER");
    }
    @GetMapping("/admin/operadores")
    public List<Map<String, String>> operadores() {
        return usuarioRepository.findByRolOrderByEmailAsc("OPERADOR").stream()
            .map(usuario -> Map.of("email", usuario.getEmail(), "rol", usuario.getRol())).collect(Collectors.toList());
    }
    @PostMapping("/admin/operadores")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> crearOperador(@RequestParam String email, @RequestParam String password) {
        return crear(email, password, "OPERADOR");
    }
    private Map<String, String> crear(String email, String password, String rol) {
        email = email.trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@\\\"]+@[^\\s@\\\"]+\\.[^\\s@\\\"]+$") || email.length() > 120
                || password.isBlank() || password.length() < 10 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisá el email y la contraseña");
        if (usuarioRepository.findByEmail(email) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya tiene una cuenta. Usá un email diferente.");
        usuarioRepository.saveAndFlush(new Usuario(email, passwordEncoder.encode(password), rol));
        return Map.of("email", email, "rol", rol);
    }
}
