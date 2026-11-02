package com.appasadito.AsaditoRg.controllers;



import com.appasadito.AsaditoRg.dtos.ProductosDTO;
import com.appasadito.AsaditoRg.repositorios.ProductosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ProductosController {

    @Autowired
    private ProductosRepository productosrepository;
    @GetMapping("/productos")
    public List<ProductosDTO> getLocales(){
        return productosrepository.findAll().stream().map(producto -> new ProductosDTO(producto)).collect(Collectors.toList());
    }

}
