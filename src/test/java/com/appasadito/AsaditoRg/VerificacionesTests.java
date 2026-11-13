package com.appasadito.AsaditoRg;

import com.appasadito.AsaditoRg.modelos.*;
import com.appasadito.AsaditoRg.repositorios.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:verificaciones;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc
@Transactional
class VerificacionesTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired LocalesRepository locales;
    @Autowired PresupuestosRepository presupuestos;
    @Autowired VerificacionesRepository verificaciones;
    @Autowired MovimientosRepository movimientos;
    Presupuestos presupuesto;

    @BeforeEach void preparar() {
        Usuario propietario = usuarios.save(new Usuario("propietario@example.com", "hash-de-prueba", "USER"));
        usuarios.save(new Usuario("otro@example.com", "hash-de-prueba", "ADMIN"));
        Locales local = locales.save(new Locales("Local de prueba", "Calle 123", "-53", "-67"));
        presupuesto = new Presupuestos();
        presupuesto.setUsuario(propietario); presupuesto.setLocal(local);
        presupuesto.setLocalnombre(local.getNombre()); presupuesto.setLocalDireccion(local.getDireccion());
        presupuesto.setTotal(20000);
        presupuestos.saveAndFlush(presupuesto);
    }
    @ParameterizedTest
    @CsvSource({"20800, ACORDE", "21000, ACORDE", "19000, ACORDE", "18000, SOBREESTIMADO", "22000, SUBESTIMADO", "21000.01, SUBESTIMADO"})
    void creaYRecuperaResultadoConLimitesExactos(String real, String valoracion) throws Exception {
        String ruta = "/api/presupuestos/" + presupuesto.getId() + "/verificacion";
        mvc.perform(post(ruta).with(user("propietario@example.com")).with(csrf())
            .param("totalReal", real).param("observaciones", "Compra de prueba"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.valoracion").value(valoracion))
            .andExpect(jsonPath("$.totalReal").value(Double.parseDouble(real)))
            .andExpect(jsonPath("$.presupuestoId").value(presupuesto.getId()))
            .andExpect(jsonPath("$.presupuesto").doesNotExist());
        mvc.perform(get(ruta).with(user("propietario@example.com")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalReal").value(Double.parseDouble(real)))
            .andExpect(jsonPath("$.observacion").value("Compra de prueba"))
            .andExpect(jsonPath("$.totalEstimado").value(20000));
        assertEquals(presupuesto.getId(), verificaciones.findByPresupuesto_Id(presupuesto.getId()).orElseThrow().getPresupuesto().getId());
        assertEquals(1, movimientos.findAll().stream().filter(m -> m.getAccion().equals("VERIFICACION_CREADA")).count());
        assertEquals(20000, presupuestos.findById(presupuesto.getId()).orElseThrow().getTotal());
    }
    @Test void protegeElDuenoYEvitaDuplicados() throws Exception {
        String ruta = "/api/presupuestos/" + presupuesto.getId() + "/verificacion";
        mvc.perform(get(ruta).with(user("propietario@example.com"))).andExpect(status().isNotFound());
        mvc.perform(post(ruta).with(user("otro@example.com").roles("ADMIN")).with(csrf()).param("totalReal", "22000")).andExpect(status().isNotFound());
        mvc.perform(post(ruta).with(user("propietario@example.com")).with(csrf()).param("totalReal", "22000")).andExpect(status().isCreated());
        mvc.perform(get(ruta).with(user("otro@example.com").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(post(ruta).with(user("propietario@example.com")).with(csrf()).param("totalReal", "18000")).andExpect(status().isConflict());
        assertEquals(22000, verificaciones.findByPresupuesto_Id(presupuesto.getId()).orElseThrow().getTotalReal());
        assertEquals(1, verificaciones.count());
    }
    @Test void validaDatosYConservaCsrf() throws Exception {
        String ruta = "/api/presupuestos/" + presupuesto.getId() + "/verificacion";
        for (String real : new String[]{"0", "-1", "NaN", "Infinity", "200.001", "1000000000000"})
            mvc.perform(post(ruta).with(user("propietario@example.com")).with(csrf()).param("totalReal", real)).andExpect(status().isBadRequest());
        mvc.perform(post(ruta).with(user("propietario@example.com")).with(csrf()).param("totalReal", "21000").param("observaciones", "x".repeat(301))).andExpect(status().isBadRequest());
        mvc.perform(post(ruta).with(user("propietario@example.com")).param("totalReal", "21000")).andExpect(status().isForbidden());
        mvc.perform(post(ruta).with(csrf()).param("totalReal", "21000")).andExpect(status().isUnauthorized());
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        assertEquals(0, verificaciones.count());
        assertEquals(0, movimientos.count());
    }
}
