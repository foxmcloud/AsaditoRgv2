package com.appasadito.AsaditoRg.modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String email;
    // Conserva la columna existente; password contiene un HASH, nunca texto plano.
    @Column(name = "password_hash", nullable = false)
    private String password;
    @Column(nullable = false)
    private String rol;

    public Usuario() {}
    public Usuario(String email, String password, String rol) {
        this.email = email; this.password = password; this.rol = rol;
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    @JsonIgnore
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
