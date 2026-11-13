package com.appasadito.AsaditoRg.dtos;

import com.appasadito.AsaditoRg.enums.ValoracionVerificacion;
import com.appasadito.AsaditoRg.modelos.Verificaciones;
import java.time.LocalDateTime;

public class VerificacionesDTO {
    private int presupuestoId;
    private double totalEstimado;
    private LocalDateTime fecha;
    private double totalReal;
    private double diferencia;
    private double porcentaje;
    private String observacion;
    private ValoracionVerificacion valoracion;

    public VerificacionesDTO(Verificaciones verificaciones) {
        this.presupuestoId = verificaciones.getPresupuesto().getId();
        this.totalEstimado = verificaciones.getPresupuesto().getTotal();
        this.fecha = verificaciones.getFecha();
        this.totalReal = verificaciones.getTotalReal();
        this.diferencia = verificaciones.getDiferencia();
        this.porcentaje = verificaciones.getPorcentaje();
        this.observacion = verificaciones.getObservacion();
        this.valoracion = verificaciones.getValoracion();
    }
    public int getPresupuestoId() { return presupuestoId; }
    public double getTotalEstimado() { return totalEstimado; }
    public LocalDateTime getFecha() { return fecha; }
    public double getTotalReal() { return totalReal; }
    public double getDiferencia() { return diferencia; }
    public double getPorcentaje() { return porcentaje; }
    public String getObservacion() { return observacion; }
    public ValoracionVerificacion getValoracion() { return valoracion; }
}
