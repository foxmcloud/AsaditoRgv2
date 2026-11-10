package com.appasadito.AsaditoRg.dtos;

import com.appasadito.AsaditoRg.modelos.Locales;
import com.appasadito.AsaditoRg.modelos.Precios;
import com.appasadito.AsaditoRg.modelos.Productos;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.LocalDate;

public class PreciosDTO {

    private int id;
    private LocalDate fecha;
    private float precio;
    private Locales local;
    private Productos producto;

    public PreciosDTO(){}

    public PreciosDTO(Precios precio){
        this.id = precio.getId();
        this.fecha = precio.getFecha();
        this.precio = precio.getPrecio();
        this.local = precio.getLocal();
        this.producto = precio.getProducto();
    }

    public int getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public float getPrecio() {
        return precio;
    }

    public Locales getLocal() {
        return local;
    }

    public Productos getProducto() {
        return producto;
    }
}
