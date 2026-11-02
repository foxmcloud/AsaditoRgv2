package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.PreciosDTO;
import com.appasadito.AsaditoRg.dtos.ProductoPrecioDTO;
import com.appasadito.AsaditoRg.modelos.Precios;
import com.appasadito.AsaditoRg.repositorios.PreciosRepository;
import com.appasadito.AsaditoRg.services.PreciosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PreciosController {

    @Autowired
    private PreciosRepository preciosrepository;
    @Autowired
    private PreciosService preciosService;

    @GetMapping("/precios")
    public List<PreciosDTO> getPrecios(){
        return preciosrepository.findAll().stream().map(precio -> new PreciosDTO(precio)).collect(Collectors.toList());
    }

    @GetMapping("/precios/ultimos")
    public List<ProductoPrecioDTO> obtenerUltimosPrecios() {
        // Llamamos al repositorio para obtener los últimos precios agrupados por local y producto
        return preciosService.obtenerUltimosPrecios();
    }

}
