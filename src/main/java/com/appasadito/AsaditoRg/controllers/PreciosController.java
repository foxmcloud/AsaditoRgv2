package com.appasadito.AsaditoRg.controllers;
import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.ProductosRepository;
import com.appasadito.AsaditoRg.repositorios.LocalesRepository;
import com.appasadito.AsaditoRg.modelos.Productos;
import com.appasadito.AsaditoRg.modelos.Locales;

import com.appasadito.AsaditoRg.dtos.PreciosDTO;
import com.appasadito.AsaditoRg.dtos.ProductoPrecioDTO;
import com.appasadito.AsaditoRg.modelos.Precios;
import com.appasadito.AsaditoRg.repositorios.PreciosRepository;
import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import com.appasadito.AsaditoRg.services.MovimientosService;
import com.appasadito.AsaditoRg.services.PreciosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PreciosController {

    @Autowired
    private PreciosRepository preciosrepository;
    @Autowired
    private LocalesRepository localesrepository;
    @Autowired
    private ProductosRepository productosrepository;
    @Autowired
    private PreciosService preciosService;
    @Autowired
    private MovimientosService movimientosService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/precios")
    public List<PreciosDTO> getPrecios(){
        return preciosrepository.findAll().stream().filter(p -> p.isActivo() && p.getLocal().isActivo() && p.getProducto().isActivo()).map(precio -> new PreciosDTO(precio)).collect(Collectors.toList());
    }

    @GetMapping("/precios/ultimos")
    public List<ProductoPrecioDTO> obtenerUltimosPrecios() {
        // Llamamos al repositorio para obtener los últimos precios agrupados por local y producto
        return preciosService.obtenerUltimosPrecios();
    }


    @PostMapping("/admin/precios")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PreciosDTO agregar(@RequestParam int localId, @RequestParam int productoId,
                             @RequestParam float precio, @RequestParam String fecha, Authentication authentication ) {

        java.time.LocalDate dia;
        try { dia = java.time.LocalDate.parse(fecha); }
        catch (java.time.format.DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha debe tener formato año-mes-día");
        }
        if (!Float.isFinite(precio) || precio <= 0 || precio > 9999999999.99 || dia.isAfter(java.time.LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio debe ser positivo y la fecha no puede ser futura");
        Locales local = localesrepository.findById(localId)
            .filter(Locales::isActivo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Local inexistente o dado de baja"));
        Productos producto = productosrepository.findById(productoId)
            .filter(Productos::isActivo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Corte inexistente o dado de baja"));
        Precios nuevo = new Precios(precio);
        nuevo.setFecha(dia);
        nuevo.setLocal(local);
        nuevo.setProducto(producto);

        Precios guardado = preciosrepository.save(nuevo);
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        System.out.println(authentication.getName());
        movimientosService.registrar(usuario, "PRECIO_PUBLICADO", "Precios", guardado.getId(),
                "Corte " + producto.getNombre() + ", local " + local.getNombre() );

        return new PreciosDTO(guardado);
    }
}
