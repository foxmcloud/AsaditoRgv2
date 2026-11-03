package com.appasadito.AsaditoRg.controllers;

import com.appasadito.AsaditoRg.dtos.CatalogRequests;
import com.appasadito.AsaditoRg.services.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping("/productos") public List<?> products() { return catalog.products(); }
    @GetMapping("/locales") public List<?> stores() { return catalog.stores(); }
    @GetMapping("/precios") public List<?> prices() { return catalog.prices(); }
    @GetMapping("/precios/ultimos") public List<?> latest() { return catalog.latest(); }
    @PostMapping("/admin/productos") @ResponseStatus(HttpStatus.CREATED)
    public Map<?, ?> addProduct(@Valid @RequestBody CatalogRequests.Producto r) { return catalog.saveProduct(null, r); }
    @PutMapping("/admin/productos/{id}")
    public Map<?, ?> editProduct(@PathVariable int id, @Valid @RequestBody CatalogRequests.Producto r) { return catalog.saveProduct(id, r); }
    @PostMapping("/admin/locales") @ResponseStatus(HttpStatus.CREATED)
    public Map<?, ?> addStore(@Valid @RequestBody CatalogRequests.Local r) { return catalog.saveStore(null, r); }
    @PutMapping("/admin/locales/{id}")
    public Map<?, ?> editStore(@PathVariable int id, @Valid @RequestBody CatalogRequests.Local r) { return catalog.saveStore(id, r); }
    @PostMapping("/admin/precios") @ResponseStatus(HttpStatus.CREATED)
    public Map<?, ?> addPrice(@Valid @RequestBody CatalogRequests.Precio r) { return catalog.savePrice(null, r); }
    @PutMapping("/admin/precios/{id}")
    public Map<?, ?> editPrice(@PathVariable int id, @Valid @RequestBody CatalogRequests.Precio r) { return catalog.savePrice(id, r); }
    @DeleteMapping("/admin/{kind:productos|locales|precios}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable String kind, @PathVariable int id) { catalog.deactivate(kind, id); }
}
