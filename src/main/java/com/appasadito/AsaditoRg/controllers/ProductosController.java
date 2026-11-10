package com.appasadito.AsaditoRg.controllers;



import com.appasadito.AsaditoRg.dtos.ProductosDTO;
import com.appasadito.AsaditoRg.modelos.Productos;
import com.appasadito.AsaditoRg.repositorios.ProductosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
        return productosrepository.findAll().stream().filter(Productos::isActivo).map(producto -> new ProductosDTO(producto)).collect(Collectors.toList());
    }


    @PostMapping("/admin/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductosDTO agregar(@RequestParam String nombre) {
        validarNombre(nombre);
        return new ProductosDTO(productosrepository.save(new Productos(nombre.trim())));
    }
    @PutMapping("/admin/productos/{id}")
    public ProductosDTO modificar(@PathVariable int id, @RequestParam String nombre) {
        validarNombre(nombre);
        Productos producto = buscar(id);
        producto.setNombre(nombre.trim());
        return new ProductosDTO(productosrepository.save(producto));
    }
    @DeleteMapping("/admin/productos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        Productos producto = buscar(id);
        producto.setActivo(false);
        productosrepository.save(producto);
    }
    private Productos buscar(int id) {
        return productosrepository.findById(id).filter(Productos::isActivo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Corte inexistente o dado de baja"));
    }
    private void validarNombre(String nombre) {
        if (nombre.trim().isEmpty() || nombre.length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresá un nombre de corte de hasta 120 caracteres");
    }
}
