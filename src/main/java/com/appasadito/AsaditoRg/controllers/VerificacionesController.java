package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.VerificacionesDTO;
import com.appasadito.AsaditoRg.enums.ValoracionVerificacion;
import com.appasadito.AsaditoRg.modelos.Presupuestos;
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
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api")
public class VerificacionesController {
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PresupuestosRepository presupuestosRepository;
    @Autowired private VerificacionesRepository verificacionesRepository;
    @Autowired private MovimientosService movimientosService;

    @GetMapping("/presupuestos/{id}/verificacion")
    @Transactional(readOnly = true)
    public VerificacionesDTO getVerificacion(@PathVariable int id, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        buscarPresupuesto(id, usuario);
        return new VerificacionesDTO(verificacionesRepository.findByPresupuesto_Id(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El presupuesto todavía no tiene verificación")));
    }

    @PostMapping("/presupuestos/{id}/verificacion")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public VerificacionesDTO agregarVerificacion(@PathVariable int id,
            @RequestParam double totalReal,
            @RequestParam(defaultValue = "") String observaciones,
            Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        Presupuestos presupuesto = buscarPresupuesto(id, usuario);
        if (verificacionesRepository.existsByPresupuesto_Id(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este presupuesto ya tiene una verificación");
        if (!Double.isFinite(totalReal) || totalReal <= 0 || totalReal > 999999999999.99
                || BigDecimal.valueOf(totalReal).stripTrailingZeros().scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresá un total pagado positivo, con hasta dos decimales");
        if (observaciones.length() > 300)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las observaciones admiten hasta 300 caracteres");
        if (!Double.isFinite(presupuesto.getTotal()) || presupuesto.getTotal() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El presupuesto no tiene un total positivo para comparar");

        // Comparar importes en centavos; el presupuesto guardado no se modifica.
        BigDecimal estimado = BigDecimal.valueOf(presupuesto.getTotal()).setScale(2, RoundingMode.HALF_UP);
        if (estimado.signum() == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El total estimado es demasiado pequeño para comparar");
        BigDecimal diferencia = BigDecimal.valueOf(totalReal).subtract(estimado);
        double porcentaje = diferencia.multiply(new BigDecimal("100"))
            .divide(estimado, 6, RoundingMode.HALF_UP).doubleValue();
        boolean acorde = diferencia.abs().multiply(new BigDecimal("100"))
            .compareTo(estimado.multiply(new BigDecimal("5"))) <= 0;

        Verificaciones verificacion = new Verificaciones();
        verificacion.setPresupuesto(presupuesto);
        verificacion.setTotalReal(totalReal);
        verificacion.setDiferencia(diferencia.doubleValue());
        verificacion.setPorcentaje(porcentaje);
        verificacion.setObservacion(observaciones.trim());
        verificacion.setValoracion(acorde ? ValoracionVerificacion.ACORDE :
            (diferencia.signum() > 0 ? ValoracionVerificacion.SUBESTIMADO : ValoracionVerificacion.SOBREESTIMADO));
        Verificaciones guardada = verificacionesRepository.saveAndFlush(verificacion);
        movimientosService.registrar(usuario, "VERIFICACION_CREADA", "Verificaciones", guardada.getId(),
            "Verificación del presupuesto " + id);
        return new VerificacionesDTO(guardada);
    }

    private Presupuestos buscarPresupuesto(int id, Usuario usuario) {
        return presupuestosRepository.findByIdAndUsuario_Id(id, usuario.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto inexistente"));
    }
}
