package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.CrearOperadorRequest;
import com.appasadito.AsaditoRg.services.OperadoresService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/admin/operadores")
public class OperadoresController {
    private final OperadoresService operators;
    public OperadoresController(OperadoresService operators) { this.operators = operators; }
    @GetMapping public List<Map<String, String>> list() { return operators.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody CrearOperadorRequest request) { return operators.create(request); }
}
