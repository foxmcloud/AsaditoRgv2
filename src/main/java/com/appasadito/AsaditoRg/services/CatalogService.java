package com.appasadito.AsaditoRg.services;

import com.appasadito.AsaditoRg.dtos.CatalogRequests;
import com.appasadito.AsaditoRg.modelos.*;
import com.appasadito.AsaditoRg.repositorios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ProductosRepository products;
    private final LocalesRepository stores;
    private final PreciosRepository prices;
    public CatalogService(ProductosRepository products, LocalesRepository stores, PreciosRepository prices) {
        this.products = products; this.stores = stores; this.prices = prices;
    }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro inexistente o dado de baja"); }
    public Productos product(int id) { return products.findById(id).filter(Productos::isActivo).orElseThrow(this::missing); }
    public Locales store(int id) { return stores.findById(id).filter(Locales::isActivo).orElseThrow(this::missing); }
    public Precios price(int id) { return prices.findById(id).filter(Precios::isActivo).orElseThrow(this::missing); }
    public Map<String, Object> productView(Productos p) { return Map.of("id", p.getId(), "nombre", p.getNombre()); }
    public Map<String, Object> storeView(Locales s) {
        return Map.of("id", s.getId(), "nombre", s.getNombre(), "direccion", s.getDireccion(),
            "coordenada_x", Objects.toString(s.getCoordenada_x(), ""), "coordenada_y", Objects.toString(s.getCoordenada_y(), ""));
    }
    public Map<String, Object> priceView(Precios p) {
        return Map.of("id", p.getId(), "precio", p.getPrecio(), "fecha", p.getFecha(), "local", storeView(p.getLocal()), "producto", productView(p.getProducto()));
    }
    public List<Map<String, Object>> products() { return products.findAll().stream().filter(Productos::isActivo).map(this::productView).collect(Collectors.toList()); }
    public List<Map<String, Object>> stores() { return stores.findAll().stream().filter(Locales::isActivo).map(this::storeView).collect(Collectors.toList()); }
    public List<Precios> activePrices() {
        return prices.findAll().stream().filter(p -> p.isActivo() && p.getLocal().isActivo() && p.getProducto().isActivo()).collect(Collectors.toList());
    }
    public List<Map<String, Object>> prices() { return activePrices().stream().map(this::priceView).collect(Collectors.toList()); }
    /** Stable IDs prevent equally named stores from being merged; ID breaks ties on the same date. */
    public List<Map<String, Object>> latest() {
        Map<Integer, Map<Integer, Precios>> grouped = new TreeMap<>();
        Comparator<Precios> order = Comparator.comparing(Precios::getFecha).thenComparingInt(Precios::getId);
        for (Precios p : activePrices()) grouped.computeIfAbsent(p.getLocal().getId(), key -> new TreeMap<>())
            .merge(p.getProducto().getId(), p, (a, b) -> order.compare(a, b) > 0 ? a : b);
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((id, entries) -> {
            Locales s = entries.values().iterator().next().getLocal();
            List<Map<String, Object>> items = entries.values().stream().map(p -> Map.<String, Object>of(
                "id", p.getProducto().getId(), "nombre", p.getProducto().getNombre(), "precio", p.getPrecio(), "fecha", p.getFecha())).collect(Collectors.toList());
            result.add(Map.of("localId", id, "localNombre", s.getNombre(), "direccion", s.getDireccion(), "productos", items));
        });
        return result;
    }
    @Transactional
    public Map<String, Object> saveProduct(Integer id, CatalogRequests.Producto request) {
        Productos p = id == null ? new Productos() : product(id);
        p.setNombre(request.nombre.trim()); return productView(products.save(p));
    }
    @Transactional
    public Map<String, Object> saveStore(Integer id, CatalogRequests.Local request) {
        Locales s = id == null ? new Locales() : store(id);
        s.setNombre(request.nombre.trim()); s.setDireccion(request.direccion.trim());
        s.setCoordenada_x(request.latitud.toPlainString()); s.setCoordenada_y(request.longitud.toPlainString());
        return storeView(stores.save(s));
    }
    @Transactional
    public Map<String, Object> savePrice(Integer id, CatalogRequests.Precio request) {
        Precios p = id == null ? new Precios() : price(id);
        p.setLocal(store(request.localId)); p.setProducto(product(request.productoId));
        p.setPrecio(request.precio); p.setFecha(request.fecha);
        return priceView(prices.save(p));
    }
    @Transactional
    public void deactivate(String kind, int id) {
        switch (kind) {
            case "productos": product(id).setActivo(false); break;
            case "locales": store(id).setActivo(false); break;
            case "precios": price(id).setActivo(false); break;
            default: throw missing();
        }
    }
}
