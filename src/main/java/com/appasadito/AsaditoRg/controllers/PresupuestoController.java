package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.MovimientosDTO;
import com.appasadito.AsaditoRg.dtos.PresupuestosDTO;
import com.appasadito.AsaditoRg.modelos.*;
import com.appasadito.AsaditoRg.repositorios.*;
import com.appasadito.AsaditoRg.services.MovimientosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PresupuestoController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private LocalesRepository localesRepository;
    @Autowired
    private ProductosRepository productosRepository;
    @Autowired
    private PreciosRepository preciosRepository;
    @Autowired
    private PresupuestosRepository presupuestosRepository;
    @Autowired
    private MovimientosService movimientosService;



    @GetMapping("/presupuestos")
    public List<PresupuestosDTO> getPresupuestos(Authentication authentication){
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        return presupuestosRepository.findByUsuario_Id(usuario.getId()).stream().map(presupuesto -> new PresupuestosDTO(presupuesto)).collect(Collectors.toList());


    }

    @GetMapping("/presupuestos/{id}")
    @Transactional(readOnly = true)
    public Map<String, Object> getPresupuesto(@PathVariable int id, Authentication authentication){
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        Presupuestos presupuesto = presupuestosRepository
                .findByIdAndUsuario_Id(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Presupuesto inexistente"
                ));

        // Convertir los detalles a una lista de Maps
        List<Map<String, Object>> detalles =
                presupuesto.getDetalles().stream()
                        .map(detalle -> {
                            Map<String, Object> item =
                                    new LinkedHashMap<>();

                            item.put("id", detalle.getId());
                            item.put("corteNombre",
                                    detalle.getCorteNombre());
                            item.put("cantidad",
                                    detalle.getCantidad());
                            item.put("precioKg",
                                    detalle.getPrecioKg());
                            item.put("subtotal",
                                    detalle.getSubtotal());

                            return item;
                        })
                        .collect(Collectors.toList());

        // Construir la respuesta del presupuesto
        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("id", presupuesto.getId());
        respuesta.put("fecha", presupuesto.getFecha());
        respuesta.put("localNombre", presupuesto.getLocalnombre());
        respuesta.put("localDireccion", presupuesto.getLocalDireccion());
        respuesta.put("moneda", presupuesto.getMoneda());
        respuesta.put("total", presupuesto.getTotal());
        respuesta.put("detalles", detalles);

        return respuesta;
    }

    @PostMapping("/presupuestos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> guardar(@RequestParam int localId, @RequestParam List<Integer> productosId,
                                       @RequestParam List<Double> cantidades, Authentication authentication) {

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        Locales local = localesRepository.findById(localId).orElseThrow();

        Double subtotal;
        Double total = 0.0;

        if (productosId.isEmpty() || cantidades.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debés seleccionar al menos un producto"
            );
        }

        if (productosId.size() != cantidades.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los productos y cantidades no coinciden"
            );
        }

        HashSet<Integer> productosSinDuplicados =
                new HashSet<>(productosId);

        if (productosSinDuplicados.size() != productosId.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No podés repetir productos"
            );
        }


        for( Integer productid : productosId){
            Productos producto = productosRepository.findById(productid)
                    .filter(Productos::isActivo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto inexistente o dado de baja"));
        }
        Presupuestos presupuestonuevo = new Presupuestos();
        presupuestonuevo.setUsuario(usuario);
        presupuestonuevo.setLocal(local);
        presupuestonuevo.setLocalnombre(local.getNombre());
        presupuestonuevo.setLocalDireccion(local.getDireccion());


        for (int i = 0; i < cantidades.size(); i++) {
            Productos producto = productosRepository.findById(productosId.get(i))
                    .filter(Productos::isActivo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto inexistente o dado de baja"));
            Precios precio = preciosRepository.findFirstByLocal_IdAndProducto_IdAndActivoTrueAndFechaLessThanEqualOrderByFechaDescIdDesc(localId, producto.getId(), LocalDate.now());
            if(precio == null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe precio para el producto");
            }
            subtotal = cantidades.get(i) * precio.getPrecio();

            DetallePresupuesto detalle = new DetallePresupuesto();
            detalle.setCantidad(cantidades.get(i));
            detalle.setFechaprecio(precio.getFecha());
            detalle.setPrecio(precio);
            detalle.setProductos(producto);
            detalle.setCorteNombre(producto.getNombre());
            detalle.setPrecioKg(precio.getPrecio());
            detalle.setSubtotal(subtotal);
            detalle.setPresupuesto(presupuestonuevo);

            presupuestonuevo.agregarDetalle(detalle);

            total = total + subtotal;
        }

        presupuestonuevo.setTotal(total);
        Presupuestos presupuestoguardado = presupuestosRepository.save(presupuestonuevo);

        movimientosService.registrar(usuario, "PRESUPUESTO_CREADO", "Presupuesto", presupuestoguardado.getId(),
                "Presupuesto creado por " + usuario.getEmail() );

        return Map.of(
                "id", presupuestoguardado.getId(),
                "total", total,
                "moneda", presupuestoguardado.getMoneda()
        );

    }
}
