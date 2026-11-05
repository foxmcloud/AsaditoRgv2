package com.appasadito.AsaditoRg.services;

import com.appasadito.AsaditoRg.dtos.CrearOperadorRequest;
import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OperadoresService {
    private final UsuarioRepository users;
    private final PasswordEncoder encoder;
    public OperadoresService(UsuarioRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }
    public List<Map<String, String>> list() {
        return users.findByRolOrderByEmailAsc("OPERADOR").stream()
            .map(user -> Map.of("email", user.getEmail(), "rol", user.getRol())).collect(Collectors.toList());
    }
    @Transactional
    public Map<String, String> create(CrearOperadorRequest request) {
        if (request.password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes UTF-8");
        String email = request.email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya tiene una cuenta. Usá un email diferente.");
        // The role is assigned by the server, never accepted from the request.
        Usuario user = users.saveAndFlush(new Usuario(email, encoder.encode(request.password), "OPERADOR"));
        return Map.of("email", user.getEmail(), "rol", user.getRol());
    }
}
