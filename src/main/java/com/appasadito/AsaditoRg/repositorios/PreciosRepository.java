package com.appasadito.AsaditoRg.repositorios;
import com.appasadito.AsaditoRg.modelos.Precios;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PreciosRepository extends JpaRepository<Precios, Integer> {
    Optional<Precios> findFirstByLocalIdAndProductoIdAndActivoTrueOrderByFechaDescIdDesc(int localId, int productoId);
}
