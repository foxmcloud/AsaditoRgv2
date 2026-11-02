package com.appasadito.AsaditoRg.dtos;

import net.bytebuddy.asm.Advice;

import java.time.LocalDate;
import java.util.List;

public class ProductoPrecioDTO {

    private String localNombre;
    private List<ProductoPrecio> productos;

    // Constructor, getters y setters
    public ProductoPrecioDTO(String localNombre, List<ProductoPrecio> productos) {
        this.localNombre = localNombre;
        this.productos = productos;
    }

    public String getLocalNombre() {
        return localNombre;
    }

    public void setLocalNombre(String localNombre) {
        this.localNombre = localNombre;
    }

    public List<ProductoPrecio> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoPrecio> productos) {
        this.productos = productos;
    }

    // Clase interna para representar el precio de un producto
    public static class ProductoPrecio {
        private String nombre;
        private double precio;
        private LocalDate fecha; // Agregamos la fecha

        public ProductoPrecio(String nombre, double precio, LocalDate fecha) {
            this.nombre = nombre;
            this.precio = precio;
            this.fecha = fecha;
        }

        // Getters y setters
        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public double getPrecio() {
            return precio;
        }

        public void setPrecio(double precio) {
            this.precio = precio;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public void setFecha(LocalDate fecha) {
            this.fecha = fecha;
        }
    }
}


