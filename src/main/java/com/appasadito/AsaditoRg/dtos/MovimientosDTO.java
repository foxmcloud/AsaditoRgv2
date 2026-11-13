package com.appasadito.AsaditoRg.dtos;

import com.appasadito.AsaditoRg.modelos.Movimientos;

import java.time.LocalDateTime;

public class MovimientosDTO {

    private int id;
    private LocalDateTime fecha;
    private String accion;
    private String entidad;
    private int registroId;
    private String resumen;


    public MovimientosDTO(){}

    public MovimientosDTO(Movimientos movimiento){
        this.id = movimiento.getId();
        this.fecha = movimiento.getFecha();
        this.accion = movimiento.getAccion();
        this.entidad = movimiento.getEntidad();
        this.registroId = movimiento.getRegistroId();
        this.resumen = movimiento.getResumen();
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public int getRegistroId() {
        return registroId;
    }

    public String getResumen() {
        return resumen;
    }
}
