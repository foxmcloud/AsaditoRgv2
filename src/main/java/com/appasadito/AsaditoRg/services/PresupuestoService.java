package com.appasadito.AsaditoRg.services;
import com.appasadito.AsaditoRg.dtos.*;
import com.appasadito.AsaditoRg.modelos.*;
import com.appasadito.AsaditoRg.repositorios.PreciosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.util.*;
@Service
public class PresupuestoService {
    private final CatalogService catalog;
    private final PreciosRepository prices;
    public PresupuestoService(CatalogService catalog, PreciosRepository prices) { this.catalog = catalog; this.prices = prices; }
    @Transactional(readOnly = true)
    public Presupuesto calculate(PresupuestoRequest request) {
        Locales store = catalog.store(request.localId);
        Set<Integer> seen = new HashSet<>();
        List<Presupuesto.Linea> lines = new ArrayList<>();
        BigDecimal total = new BigDecimal("0.00");
        for (PresupuestoRequest.Item item : request.items) {
            if (!seen.add(item.productoId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No repitas un corte en el presupuesto");
            Productos product = catalog.product(item.productoId);
            Precios price = prices.findFirstByLocalIdAndProductoIdAndActivoTrueOrderByFechaDescIdDesc(store.getId(), product.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Falta precio para " + product.getNombre() + " en ese local"));
            BigDecimal subtotal = price.getPrecio().multiply(item.cantidad).setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);
            lines.add(new Presupuesto.Linea(product.getId(), product.getNombre(), item.cantidad, price.getPrecio(), subtotal, price.getFecha()));
        }
        return new Presupuesto(store.getId(), store.getNombre(), store.getDireccion(), lines, total);
    }
}
