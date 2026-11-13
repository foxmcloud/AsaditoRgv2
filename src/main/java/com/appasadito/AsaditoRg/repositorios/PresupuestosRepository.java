package com.appasadito.AsaditoRg.repositorios;

import com.appasadito.AsaditoRg.modelos.Presupuestos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestosRepository extends JpaRepository<Presupuestos, Integer> {

    Optional<Presupuestos> findByIdAndUsuario_Id(int id, Long usuarioId);
    List<Presupuestos> findByIdUsuario_Id(Long usuarioId);

}
