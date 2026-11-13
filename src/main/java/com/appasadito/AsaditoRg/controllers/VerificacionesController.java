package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.VerificacionesDTO;
import com.appasadito.AsaditoRg.enums.ValoracionVerificacion;
import com.appasadito.AsaditoRg.modelos.Presupuestos;
import com.appasadito.AsaditoRg.modelos.Productos;
import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.modelos.Verificaciones;
import com.appasadito.AsaditoRg.repositorios.PresupuestosRepository;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import com.appasadito.AsaditoRg.repositorios.VerificacionesRepository;
import com.appasadito.AsaditoRg.services.MovimientosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static java.lang.Math.abs;

@RestController
@RequestMapping("/api")
public class VerificacionesController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PresupuestosRepository presupuestosRepository;
    @Autowired
    private VerificacionesRepository verificacionesRepository;
    @Autowired
    private MovimientosService movimientosService;

    @GetMapping("/presupuestos/{id}/verificacion")
    @Transactional(readOnly = true)
    public VerificacionesDTO getVerificacion(@PathVariable int id, Authentication authentication){

        if(!verificacionesRepository.existsByPresupuesto_Id(id)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No existe verificacion sobre este presupuesto"
            );

        }
        return new VerificacionesDTO(verificacionesRepository.findByPresupuesto_Id(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "")));
    }

    @PostMapping("/presupuestos/{id}/verificacion")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public VerificacionesDTO agregarVerificacion(@PathVariable int id, @PathVariable Float totalReal, @PathVariable String observaciones, Authentication authentication){

        if(verificacionesRepository.existsByPresupuesto_Id(id)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ya existe una verificacion sobre este presupuesto"
            );
        }

        Presupuestos presupuesto = presupuestosRepository.findById(id).orElseThrow();

        Verificaciones verificacion = new Verificaciones();
        verificacion.setTotalReal(totalReal);
        verificacion.setDiferencia(totalReal - presupuesto.getTotal());
        verificacion.setPorcentaje((verificacion.getDiferencia() / presupuesto.getTotal()) * 100);
        verificacion.setObservacion(observaciones);


        if(verificacion.getPorcentaje() <= 5 & verificacion.getPorcentaje() >=-5){
            verificacion.setValoracion(ValoracionVerificacion.ACORDE);
        } else if (verificacion.getPorcentaje() > 5){
            verificacion.setValoracion(ValoracionVerificacion.SUBESTIMADO);
        } else {
            verificacion.setValoracion(ValoracionVerificacion.SOBREESTIMADO);
        }

        Verificaciones verificacionguardada = verificacionesRepository.save(verificacion);

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());

        movimientosService.registrar(usuario, "VERIFICACION_CREADA", "Verificaciones", verificacionguardada.getId(),
                "Verificacion creada por " + usuario.getEmail() );

        return new VerificacionesDTO(verificacionguardada);
    }
}
