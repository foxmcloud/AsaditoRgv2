package com.appasadito.AsaditoRg.dtos;

import com.appasadito.AsaditoRg.enums.ValoracionVerificacion;
import com.appasadito.AsaditoRg.modelos.Presupuestos;
import com.appasadito.AsaditoRg.modelos.Verificaciones;

import java.time.LocalDateTime;

public class VerificacionesDTO {

    private Presupuestos presupuesto;

    private LocalDateTime fecha;
    private double totalReal;
    private double diferencia;
    private double porcentaje;
    private String observacion;
    private ValoracionVerificacion valoracion;

    public VerificacionesDTO(Verificaciones verificaciones){
        this.presupuesto = verificaciones.getPresupuesto();
        this.fecha = verificaciones.getFecha();
        this.diferencia = verificaciones.getDiferencia();
        this.porcentaje = verificaciones.getPorcentaje();
        this.observacion = verificaciones.getObservacion();
        this.valoracion = verificaciones.getValoracion();
    }

    public Presupuestos getPresupuesto() {
        return presupuesto;
    }

    public ValoracionVerificacion getValoracion() {
        return valoracion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public double getTotalReal() {
        return totalReal;
    }

    public double getDiferencia() {
        return diferencia;
    }

    public double getPorcentaje() {
        return porcentaje;
    }

    public String getObservacion() {
        return observacion;
    }
}
