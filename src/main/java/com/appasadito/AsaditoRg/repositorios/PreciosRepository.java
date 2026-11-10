package com.appasadito.AsaditoRg.repositorios;

import com.appasadito.AsaditoRg.dtos.PreciosDTO;
import com.appasadito.AsaditoRg.dtos.ProductoPrecioDTO;
import com.appasadito.AsaditoRg.modelos.Precios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface PreciosRepository extends JpaRepository<Precios, Integer> {

    @Query("SELECT p.local.nombre AS localNombre, p.producto.nombre AS productoNombre, p.precio AS precio, p.fecha AS fecha, p.local.direccion AS direccion " +
            "FROM Precios p " +
            "WHERE p.activo = true AND p.local.activo = true AND p.producto.activo = true AND p.fecha = (SELECT MAX(p2.fecha) FROM Precios p2 " +
            "WHERE p2.activo = true AND p2.producto.id = p.producto.id AND p2.local.id = p.local.id) " +
            "ORDER BY p.local.nombre, p.producto.nombre")
    List<Object[]> obtenerUltimosPrecios();

}
