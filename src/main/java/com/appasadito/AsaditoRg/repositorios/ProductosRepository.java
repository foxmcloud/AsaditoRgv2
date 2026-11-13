package com.appasadito.AsaditoRg.repositorios;

import com.appasadito.AsaditoRg.modelos.Presupuestos;
import com.appasadito.AsaditoRg.modelos.Productos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductosRepository extends JpaRepository<Productos, Integer> {

    Optional<Productos> findByIdAndActivoTrue(Integer productid);
}
