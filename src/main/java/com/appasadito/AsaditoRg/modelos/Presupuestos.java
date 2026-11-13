package com.appasadito.AsaditoRg.modelos;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
public class Presupuestos {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_id")
    private Locales local;

    private LocalDateTime fecha = LocalDateTime.now();
    private String localnombre;
    private String localDireccion;
    private String moneda = "ARS";
    private double total;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<DetallePresupuesto> detalles = new ArrayList<>();

    public Presupuestos(){}

    public void agregarDetalle(DetallePresupuesto detalle){
        detalle.setPresupuesto(this);
        detalles.add(detalle);
    }

    public int getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Locales getLocal() {
        return local;
    }

    public void setLocal(Locales local) {
        this.local = local;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getLocalnombre() {
        return localnombre;
    }

    public void setLocalnombre(String localnombre) {
        this.localnombre = localnombre;
    }

    public String getLocalDireccion() {
        return localDireccion;
    }

    public void setLocalDireccion(String localDireccion) {
        this.localDireccion = localDireccion;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<DetallePresupuesto> getDetalles() {
        return detalles;
    }

}
