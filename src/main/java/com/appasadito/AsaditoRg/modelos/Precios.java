package com.appasadito.AsaditoRg.modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class Precios {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;
    private LocalDate fecha;
    private float precio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="locales_id")
    private Locales local;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="productos_id")
    private Productos producto;

    public Precios(){}

    public Precios(float precio){
        this.fecha = LocalDate.now();
        this.precio = precio;
    }

    public int getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public float getPrecio() {
        return precio;
    }

    public void setPrecio(float precio) {
        this.precio = precio;
    }
    @JsonIgnore
    public Locales getLocal() {
        return local;
    }

    public void setLocal(Locales local) {
        this.local = local;
    }
    @JsonIgnore
    public Productos getProducto() {
        return producto;
    }

    public void setProducto(Productos producto) {
        this.producto = producto;
    }
}
