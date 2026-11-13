package com.appasadito.AsaditoRg.controllers;



import com.appasadito.AsaditoRg.dtos.ProductosDTO;
import com.appasadito.AsaditoRg.modelos.Productos;
import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.ProductosRepository;

import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import com.appasadito.AsaditoRg.services.MovimientosService;
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
public class ProductosController {

    @Autowired
    private ProductosRepository productosrepository;
    @Autowired
    private MovimientosService movimientosService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/productos")
    public List<ProductosDTO> getLocales(){
        return productosrepository.findAll().stream().filter(Productos::isActivo).map(producto -> new ProductosDTO(producto)).collect(Collectors.toList());
    }

    @PostMapping("/admin/productos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductosDTO agregar(@RequestParam String nombre, Authentication authentication) {
        validarNombre(nombre);
        Productos productoguardado = productosrepository.save(new Productos(nombre.trim()));
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        movimientosService.registrar(usuario, "PRODUCTO_CREADO", "Productos", productoguardado.getId(),
                "Usuario "+ usuario.getEmail() +" crea Producto " + productoguardado.getNombre() );

        return new ProductosDTO(productoguardado);
    }

    @PutMapping("/admin/productos/{id}")
    @Transactional
    public ProductosDTO modificar(@PathVariable int id, @RequestParam String nombre, Authentication authentication) {
        validarNombre(nombre);
        Productos producto = buscar(id);
        producto.setNombre(nombre.trim());
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        Productos productoModificado = productosrepository.save(producto);
        movimientosService.registrar(usuario, "PRODUCTO_MODIFICADO", "Productos", productoModificado.getId(),
                "Usuario "+ usuario.getEmail() +" Modifica datos de Producto " + productoModificado.getNombre() );

        return new ProductosDTO(productoModificado);
    }
    @DeleteMapping("/admin/productos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void eliminar(@PathVariable int id, Authentication authentication) {
        Productos producto = buscar(id);
        producto.setActivo(false);
        productosrepository.save(producto);
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
        movimientosService.registrar(usuario, "PRODUCTO_ELIMINADO", "Productos", producto.getId(),
                "Usuario "+ usuario.getEmail() +" Elimina Producto " + producto.getNombre() );

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
