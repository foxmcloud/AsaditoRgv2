package com.appasadito.AsaditoRg.repositorios;

import com.appasadito.AsaditoRg.modelos.Movimientos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientosRepository extends JpaRepository<Movimientos, Integer> {

    Page<Movimientos> findAllByOrderByFechaDescIdDesc(Pageable pageable);
}
