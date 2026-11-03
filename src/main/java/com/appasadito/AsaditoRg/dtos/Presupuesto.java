package com.appasadito.AsaditoRg.dtos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
/** Server-calculated snapshot shared by JSON and PDF. */
public class Presupuesto {
    public final int localId;
    public final String localNombre;
    public final String direccion;
    public final List<Linea> items;
    public final BigDecimal total;
    public final String moneda = "ARS";
    public final LocalDate generado = LocalDate.now();
    public Presupuesto(int id, String name, String address, List<Linea> items, BigDecimal total) {
        this.localId = id; this.localNombre = name; this.direccion = address;
        this.items = List.copyOf(items); this.total = total;
    }
    public static class Linea {
        public final int productoId;
        public final String nombre;
        public final BigDecimal cantidad;
        public final BigDecimal precio;
        public final BigDecimal subtotal;
        public final LocalDate fecha;
        public Linea(int id, String name, BigDecimal quantity, BigDecimal price, BigDecimal subtotal, LocalDate date) {
            this.productoId = id; this.nombre = name; this.cantidad = quantity;
            this.precio = price; this.subtotal = subtotal; this.fecha = date;
        }
    }
}
