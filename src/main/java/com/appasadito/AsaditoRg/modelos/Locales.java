package com.appasadito.AsaditoRg.modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Locales {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;
    private String nombre;
    private String direccion;
    private String coordenada_x;
    private String coordenada_y;

    @OneToMany(mappedBy = "local", fetch = FetchType.EAGER)
    Set<Precios> precios = new HashSet<>();

    public Locales(){}

    public Locales(String nombre, String direccion, String coordenada_x, String coordenada_y){

        this.nombre = nombre;
        this.direccion = direccion;
        this.coordenada_x = coordenada_x;
        this.coordenada_y = coordenada_y;

    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCoordenada_x() {
        return coordenada_x;
    }

    public void setCoordenada_x(String coordenada_x) {
        this.coordenada_x = coordenada_x;
    }

    public String getCoordenada_y() {
        return coordenada_y;
    }

    public void setCoordenada_y(String coordenada_y) {
        this.coordenada_y = coordenada_y;
    }

    public void addPrecio(Precios precio) {
        precio.setLocal(this);
        precios.add(precio);
    }
    @JsonIgnore
    public Set<Precios> getPrecios() {
        return precios;
    }

    public void setPrecios(Set<Precios> precios) {
        this.precios = precios;
    }
}
