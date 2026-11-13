package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.MovimientosDTO;
import com.appasadito.AsaditoRg.dtos.ProductosDTO;
import com.appasadito.AsaditoRg.modelos.Productos;
import com.appasadito.AsaditoRg.repositorios.MovimientosRepository;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MovimientosController {

    @Autowired
    private MovimientosRepository movimientosRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/admin/movimientos")
    public List<MovimientosDTO> getMovimientos(){
        return movimientosRepository.findAll().stream().map(movimiento -> new MovimientosDTO(movimiento)).collect(Collectors.toList());
    }
}
