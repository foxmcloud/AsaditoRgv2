package com.appasadito.AsaditoRg.modelos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Productos {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;
    private String nombre;

    @OneToMany(mappedBy = "producto", fetch = FetchType.EAGER)
    Set<Precios> precios = new HashSet<>();

    public Productos(){}

    public Productos(String nombre){
        this.nombre = nombre;
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

    public void addPrecio(Precios precio) {
        precio.setProducto(this);
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
