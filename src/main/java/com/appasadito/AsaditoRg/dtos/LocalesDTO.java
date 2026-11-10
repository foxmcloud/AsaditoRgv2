package com.appasadito.AsaditoRg.dtos;

import com.appasadito.AsaditoRg.modelos.Locales;
import com.appasadito.AsaditoRg.modelos.Precios;

import java.util.HashSet;
import java.util.Set;

public class LocalesDTO {
    private int id;
    private String nombre;
    private String direccion;
    private String coordenada_x;
    private String coordenada_y;
    private Set<Precios> precios = new HashSet<>();

    public LocalesDTO(){}

    public LocalesDTO(Locales local){
        this.id = local.getId();
        this.nombre = local.getNombre();
        this.direccion = local.getDireccion();
        this.coordenada_x = local.getCoordenada_x();
        this.coordenada_y = local.getCoordenada_y();
        this.precios = local.getPrecios();
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCoordenada_x() {
        return coordenada_x;
    }

    public String getCoordenada_y() {
        return coordenada_y;
    }

    public Set<Precios> getPrecios() {
        return precios;
    }
}
