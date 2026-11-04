package com.appasadito.AsaditoRg.dtos;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public class PresupuestoRequest {
    @Min(1) public int localId;
        public int getLocalId() { return localId; }
    @NotEmpty @Size(max = 100) public List<@NotNull @Valid Item> items;
    public List<Item> getItems() { return items; }
    public static class Item {
        @Min(1) public int productoId;
        public int getProductoId() { return productoId; }
        @NotNull @DecimalMin("0.5") @DecimalMax("1000")
        public BigDecimal cantidad;
        public BigDecimal getCantidad() { return cantidad; }
        @com.fasterxml.jackson.annotation.JsonIgnore
        @AssertTrue(message = "La cantidad debe aumentar de a 0,5 kg")
        public boolean isCantidadEnMediosKilos() {
            return cantidad == null || cantidad.remainder(new BigDecimal("0.5")).signum() == 0;
        }
    }
}
