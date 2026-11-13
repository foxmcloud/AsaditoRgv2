package com.appasadito.AsaditoRg.modelos;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class DetallePresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id")
    private Presupuestos presupuesto;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Productos productos;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "precio_id")
    private Precios precio;

    private String corteNombre;
    private double cantidad;
    private float precioKg;
    private LocalDate fechaprecio;
    private double subtotal;

    public DetallePresupuesto(){}

    public int getId() {
        return id;
    }

    public Presupuestos getPresupuesto() {
        return presupuesto;
    }


    public Productos getProductos() {
        return productos;
    }

    public void setProductos(Productos productos) {
        this.productos = productos;
    }

    public Precios getPrecio() {
        return precio;
    }

    public void setPrecio(Precios precio) {
        this.precio = precio;
    }

    public String getCorteNombre() {
        return corteNombre;
    }

    public void setCorteNombre(String corteNombre) {
        this.corteNombre = corteNombre;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public float getPrecioKg() {
        return precioKg;
    }

    public void setPrecioKg(float precioKg) {
        this.precioKg = precioKg;
    }

    public LocalDate getFechaprecio() {
        return fechaprecio;
    }

    public void setFechaprecio(LocalDate fechaprecio) {
        this.fechaprecio = fechaprecio;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public void setPresupuesto(Presupuestos presupuesto) {
        this.presupuesto = presupuesto;
    }
}
