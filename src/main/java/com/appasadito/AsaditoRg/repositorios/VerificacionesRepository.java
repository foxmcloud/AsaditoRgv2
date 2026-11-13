package com.appasadito.AsaditoRg.repositorios;

import com.appasadito.AsaditoRg.modelos.Verificaciones;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionesRepository extends JpaRepository<Verificaciones, Integer> {

    boolean existsByPresupuesto_Id(Integer presupuestoId);
    Optional<Verificaciones> findByPresupuesto_Id(Integer presupuestoId);

}
