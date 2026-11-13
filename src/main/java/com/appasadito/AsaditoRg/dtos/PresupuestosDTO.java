package com.appasadito.AsaditoRg.dtos;

import com.appasadito.AsaditoRg.modelos.Locales;
import com.appasadito.AsaditoRg.modelos.Presupuestos;
import com.appasadito.AsaditoRg.modelos.Usuario;

import java.time.LocalDateTime;

public class PresupuestosDTO {

    private int id;
    private LocalDateTime fecha;
    private String localnombre;
    private String localDireccion;
    private String moneda;
    private double total;

    public PresupuestosDTO(Presupuestos presupuestos){
        this.id = presupuestos.getId();
        this.fecha = presupuestos.getFecha();
        this.localnombre = presupuestos.getLocalnombre();
        this.localDireccion = presupuestos.getLocalDireccion();
        this.moneda = presupuestos.getMoneda();
        this.total = presupuestos.getTotal();
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getLocalnombre() {
        return localnombre;
    }

    public String getLocalDireccion() {
        return localDireccion;
    }

    public String getMoneda() {
        return moneda;
    }

    public double getTotal() {
        return total;
    }
}
