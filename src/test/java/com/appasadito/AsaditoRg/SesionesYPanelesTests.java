package com.appasadito.AsaditoRg;

import com.appasadito.AsaditoRg.modelos.*;
import com.appasadito.AsaditoRg.repositorios.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:sesiones;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc
@Transactional
class SesionesYPanelesTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductosRepository productos;
    @Autowired LocalesRepository locales;
    @Autowired PreciosRepository precios;
    @Autowired PasswordEncoder encoder;
    Productos corte;
    Locales local;
    @BeforeEach void preparar() {
        corte = productos.save(new Productos("Asado de prueba"));
        local = locales.save(new Locales("Local de prueba", "Calle 123", "-53.7", "-67.7"));
    }
    @Test void mantieneLosDtoOriginalesYCopiaElPrecio() throws Exception {
        Precios precio = new Precios(25000); precio.setLocal(local); precio.setProducto(corte); precios.saveAndFlush(precio);
        mvc.perform(get("/api/precios")).andExpect(status().isOk()).andExpect(jsonPath("$[0].precio").value(25000))
            .andExpect(jsonPath("$[0].local.id").value(local.getId())).andExpect(jsonPath("$[0].producto.id").value(corte.getId()));
        mvc.perform(get("/api/precios/ultimos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].productos[0].precio").value(25000));
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/locales")).andExpect(status().isOk());
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void iniciaYCierraSesionConHashesNuevosYAnteriores(boolean hashAnterior) throws Exception {
        String hash = hashAnterior ? new BCryptPasswordEncoder().encode("UnaClaveSegura123") : encoder.encode("UnaClaveSegura123");
        usuarios.saveAndFlush(new Usuario("operador@example.com", hash, "OPERADOR"));
        MockHttpSession anterior = new MockHttpSession(); String idAnterior = anterior.getId();
        MvcResult login = mvc.perform(post("/api/login").session(anterior).with(csrf())
            .param("email", "operador@example.com").param("password", "UnaClaveSegura123"))
            .andExpect(status().isNoContent()).andReturn();
        MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);
        assertNotEquals(idAnterior, sesion.getId());
        mvc.perform(get("/api/auth/me").session(sesion)).andExpect(jsonPath("$.operador").value(true)).andExpect(jsonPath("$.admin").value(false));
        mvc.perform(post("/api/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());
        assertTrue(sesion.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(jsonPath("$.authenticated").value(false));
    }
    @Test void rechazaPasswordIncorrectoYConservaCsrf() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).param("email", "nadie@example.com").param("password", "incorrecta")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").param("email", "nadie@example.com").param("password", "incorrecta")).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings = {"USER", "OPERADOR", "ADMIN"})
    void permisosDeCadaPanel(String rol) throws Exception {
        mvc.perform(post("/api/admin/productos").with(user("cuenta").authorities(new SimpleGrantedAuthority(rol))).with(csrf()).param("nombre", "Corte nuevo"))
            .andExpect(status().is(rol.equals("ADMIN") ? 201 : 403));
        mvc.perform(put("/api/admin/productos/" + corte.getId()).with(user("cuenta").authorities(new SimpleGrantedAuthority(rol))).with(csrf()).param("nombre", "Corte editado"))
            .andExpect(status().is(rol.equals("ADMIN") ? 200 : 403));
        mvc.perform(post("/api/admin/precios").with(user("cuenta").authorities(new SimpleGrantedAuthority(rol))).with(csrf())
            .param("localId", "" + local.getId()).param("productoId", "" + corte.getId()).param("precio", "12500.50").param("fecha", LocalDate.now().toString()))
            .andExpect(status().is(rol.equals("OPERADOR") ? 201 : 403));
        mvc.perform(delete("/api/admin/productos/" + corte.getId()).with(user("cuenta").authorities(new SimpleGrantedAuthority(rol))).with(csrf()))
            .andExpect(status().is(rol.equals("ADMIN") ? 204 : 403));
        assertEquals(!rol.equals("ADMIN"), productos.findById(corte.getId()).orElseThrow().isActivo());
        mvc.perform(get("/api/admin/operadores").with(user("cuenta").authorities(new SimpleGrantedAuthority(rol))))
            .andExpect(status().is(rol.equals("ADMIN") ? 200 : 403));
    }
    @Test void adminCreaOperadorConPasswordHasheado() throws Exception {
        mvc.perform(post("/api/admin/operadores").with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))).with(csrf())
            .param("email", "Nuevo@example.com").param("password", "UnaClaveSegura123").param("rol", "ADMIN"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("OPERADOR"))
            .andExpect(jsonPath("$.password").doesNotExist());
        Usuario nuevo = usuarios.findByEmail("nuevo@example.com");
        assertEquals("OPERADOR", nuevo.getRol()); assertTrue(encoder.matches("UnaClaveSegura123", nuevo.getPassword()));
        mvc.perform(post("/api/admin/operadores").with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))).with(csrf())
            .param("email", "nuevo@example.com").param("password", "OtraClaveSegura123")).andExpect(status().isConflict());
        assertTrue(encoder.matches("UnaClaveSegura123", usuarios.findByEmail("nuevo@example.com").getPassword()));
    }
    @Test void validaCamposEnJava() throws Exception {
        mvc.perform(post("/api/admin/productos").with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))).with(csrf()).param("nombre", " ")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/locales").with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))).with(csrf())
            .param("nombre", "Local").param("direccion", "Calle").param("latitud", "91").param("longitud", "0")).andExpect(status().isBadRequest());
        for (String valor : new String[]{"-1", "NaN"}) mvc.perform(post("/api/admin/precios").with(user("op").authorities(new SimpleGrantedAuthority("OPERADOR"))).with(csrf())
            .param("localId", "" + local.getId()).param("productoId", "" + corte.getId()).param("precio", valor).param("fecha", LocalDate.now().toString())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/registro").with(csrf()).param("email", "mal").param("password", "corta")).andExpect(status().isBadRequest());
    }
    @Test void registroPublicoNoPermiteCrearAdministradores() throws Exception {
        mvc.perform(post("/api/auth/registro").with(csrf()).param("email", "cliente@example.com").param("password", "UnaClaveSegura123").param("rol", "ADMIN"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("USER"));
        mvc.perform(get("/api/admin/operadores")).andExpect(status().isUnauthorized());
    }
}
