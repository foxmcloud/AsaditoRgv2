package com.appasadito.AsaditoRg.modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Movimientos {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fecha = LocalDateTime.now();
    private String accion;
    private String entidad;
    private int registroId;
    private String resumen;


    public Movimientos(){}

    public Movimientos(Usuario usuario, LocalDateTime fecha, String accion, String entidad, int registroId, String resumen){
        this.usuario = usuario;
        this.fecha = fecha;
        this.accion = accion;
        this.entidad = entidad;
        this.registroId = registroId;
        this.resumen = resumen;
    }

    public int getId() {
        return id;
    }

    @JsonIgnore
    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFecha(){
        return fecha;
    }

    public String getAccion(){
        return accion;
    }

    public String getEntidad(){
        return entidad;
    }

    public int getRegistroId(){
        return registroId;
    }

    public String getResumen(){
        return resumen;
    }

    public void setFecha(LocalDateTime fecha){
        this.fecha = fecha;
    }

    public void setAccion(String accion){
        this.accion = accion;
    }

    public void setEntidad(String entidad){
        this.entidad = entidad;
    }

    public void setRegistroId(int registroId){
        this.registroId = registroId;
    }

    public void setResumen(String resumen){
        this.resumen = resumen;
    }

}
