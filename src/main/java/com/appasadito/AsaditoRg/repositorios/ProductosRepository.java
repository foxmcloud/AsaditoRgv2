package com.appasadito.AsaditoRg.repositorios;

import com.appasadito.AsaditoRg.modelos.Productos;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductosRepository extends JpaRepository<Productos , Integer> {

    Productos findByIdAndIsActivo(Integer productid);
}
