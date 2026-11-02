package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.LocalesDTO;
import com.appasadito.AsaditoRg.modelos.Locales;
import com.appasadito.AsaditoRg.repositorios.LocalesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
        return localesrepository.findAll().stream().map(local -> new LocalesDTO(local)).collect(Collectors.toList());
    }

}
