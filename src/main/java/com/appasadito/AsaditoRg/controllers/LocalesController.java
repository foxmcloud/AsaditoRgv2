package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.LocalesDTO;
import com.appasadito.AsaditoRg.modelos.Locales;
import com.appasadito.AsaditoRg.repositorios.LocalesRepository;
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
public class LocalesController {

    @Autowired
    private LocalesRepository localesrepository;
    @GetMapping("/locales")
    public List<LocalesDTO> getLocales(){
        return localesrepository.findAll().stream().filter(Locales::isActivo).map(local -> new LocalesDTO(local)).collect(Collectors.toList());
    }


    @PostMapping("/admin/locales")
    @ResponseStatus(HttpStatus.CREATED)
    public LocalesDTO agregar(@RequestParam String nombre, @RequestParam String direccion,
                              @RequestParam double latitud, @RequestParam double longitud) {
        validar(nombre, direccion, latitud, longitud);
        Locales local = new Locales(nombre.trim(), direccion.trim(), String.valueOf(latitud), String.valueOf(longitud));
        return new LocalesDTO(localesrepository.save(local));
    }
    @PutMapping("/admin/locales/{id}")
    public LocalesDTO modificar(@PathVariable int id, @RequestParam String nombre, @RequestParam String direccion,
                                @RequestParam double latitud, @RequestParam double longitud) {
        validar(nombre, direccion, latitud, longitud);
        Locales local = buscar(id);
        local.setNombre(nombre.trim()); local.setDireccion(direccion.trim());
        local.setCoordenada_x(String.valueOf(latitud)); local.setCoordenada_y(String.valueOf(longitud));
        return new LocalesDTO(localesrepository.save(local));
    }
    @DeleteMapping("/admin/locales/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        Locales local = buscar(id); local.setActivo(false); localesrepository.save(local);
    }
    private Locales buscar(int id) {
        return localesrepository.findById(id).filter(Locales::isActivo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Local inexistente o dado de baja"));
    }
    private void validar(String nombre, String direccion, double latitud, double longitud) {
        if (nombre.trim().isEmpty() || nombre.length() > 120 || direccion.trim().isEmpty() || direccion.length() > 200
                || !Double.isFinite(latitud) || !Double.isFinite(longitud) || Math.abs(latitud) > 90 || Math.abs(longitud) > 180)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisá nombre, dirección y coordenadas del local");
    }
}
