package com.appasadito.AsaditoRg.modelos;

import com.appasadito.AsaditoRg.enums.ValoracionVerificacion;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Verificaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id", unique = true)
    private Presupuestos presupuesto;

    private LocalDateTime fecha = LocalDateTime.now();
    private double totalReal;
    private double diferencia;
    private double porcentaje;
    private String observacion;
    private ValoracionVerificacion valoracion;


    public Verificaciones(){}

    public int getId() {
        return id;
    }

    public Presupuestos getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(Presupuestos presupuesto) {
        this.presupuesto = presupuesto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public double getTotalReal() {
        return totalReal;
    }

    public void setTotalReal(double totalReal) {
        this.totalReal = totalReal;
    }

    public double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(double diferencia) {
        this.diferencia = diferencia;
    }

    public double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }


    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public ValoracionVerificacion getValoracion() {
        return valoracion;
    }

    public void setValoracion(ValoracionVerificacion valoracion) {
        this.valoracion = valoracion;
    }

}
