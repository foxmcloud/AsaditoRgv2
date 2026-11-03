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
        @NotNull @DecimalMin("0.001") @DecimalMax("1000") @Digits(integer = 4, fraction = 3)
        public BigDecimal cantidad;
        public BigDecimal getCantidad() { return cantidad; }
    }
}
