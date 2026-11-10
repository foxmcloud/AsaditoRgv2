package com.appasadito.AsaditoRg.repositorios;

import com.appasadito.AsaditoRg.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByEmail(String email);
    List<Usuario> findByRolOrderByEmailAsc(String rol);
}
