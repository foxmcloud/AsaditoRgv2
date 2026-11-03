package com.appasadito.AsaditoRg.dtos;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class CatalogRequests {
    private CatalogRequests() { }
    public static class Producto {
        @NotBlank @Size(max = 120) public String nombre;
        public String getNombre() { return nombre; }
    }
    public static class Local {
        @NotBlank @Size(max = 120) public String nombre;
        public String getNombre() { return nombre; }
        @NotBlank @Size(max = 200) public String direccion;
        public String getDireccion() { return direccion; }
        @NotNull @DecimalMin("-90") @DecimalMax("90") public BigDecimal latitud;
        public BigDecimal getLatitud() { return latitud; }
        @NotNull @DecimalMin("-180") @DecimalMax("180") public BigDecimal longitud;
        public BigDecimal getLongitud() { return longitud; }
    }
    public static class Precio {
        @Min(1) public int localId;
        public int getLocalId() { return localId; }
        @Min(1) public int productoId;
        public int getProductoId() { return productoId; }
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) public BigDecimal precio;
        public BigDecimal getPrecio() { return precio; }
        @NotNull @PastOrPresent public LocalDate fecha;
        public LocalDate getFecha() { return fecha; }
    }
}
