package com.appasadito.AsaditoRg.dtos;

import com.appasadito.AsaditoRg.modelos.Precios;
import com.appasadito.AsaditoRg.modelos.Productos;

import java.util.HashSet;
import java.util.Set;

public class ProductosDTO {
    private int id;
    private String nombre;
    Set<Precios> precios = new HashSet<>();

    public ProductosDTO(){}

    public ProductosDTO(Productos producto){
        this.id = producto.getId();
        this.nombre = producto.getNombre();
        this.precios = producto.getPrecios();
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Set<Precios> getPrecios() {
        return precios;
    }
}
