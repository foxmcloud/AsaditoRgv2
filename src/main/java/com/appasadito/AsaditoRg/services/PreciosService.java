package com.appasadito.AsaditoRg.services;

import com.appasadito.AsaditoRg.dtos.ProductoPrecioDTO;
import com.appasadito.AsaditoRg.repositorios.PreciosRepository;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

    @Service
    public class PreciosService {

        @Autowired
        private PreciosRepository precioRepository;

        public List<ProductoPrecioDTO> obtenerUltimosPrecios() {
            List<Object[]> result = precioRepository.obtenerUltimosPrecios();
            Map<String, List<ProductoPrecioDTO.ProductoPrecio>> localProductoMap = new LinkedHashMap<>();

            for (Object[] row : result) {
                String localNombre = (String) row[0];
                String productoNombre = (String) row[1];
                float precio = (float) row[2];
                LocalDate fecha = (LocalDate) row[3];  // Ahora obtenemos la fecha

                // Si el local no está en el mapa, lo añadimos
                localProductoMap.computeIfAbsent(localNombre, k -> new ArrayList<>())
                        .add(new ProductoPrecioDTO.ProductoPrecio(productoNombre, precio, fecha));
            }

            // Convertimos el mapa en una lista de DTOs
            List<ProductoPrecioDTO> productoPrecioDTOList = new ArrayList<>();
            for (Map.Entry<String, List<ProductoPrecioDTO.ProductoPrecio>> entry : localProductoMap.entrySet()) {
                productoPrecioDTOList.add(new ProductoPrecioDTO(entry.getKey(), entry.getValue()));
            }

            return productoPrecioDTOList;
        }
    }

