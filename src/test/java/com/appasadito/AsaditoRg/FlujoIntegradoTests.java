package com.appasadito.AsaditoRg;

import com.appasadito.AsaditoRg.modelos.*;
import com.appasadito.AsaditoRg.repositorios.*;
import com.appasadito.AsaditoRg.services.PdfService;
import com.appasadito.AsaditoRg.dtos.Presupuesto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.appasadito.AsaditoRg.config.AdminBootstrap;
import javax.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:FlujoIntegradoTests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc
@Transactional
class FlujoIntegradoTests {
    @Autowired MockMvc mvc;
    @Autowired ProductosRepository products;
    @Autowired LocalesRepository stores;
    @Autowired PreciosRepository prices;
    @Autowired UsuarioRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired PdfService pdf;
    @Autowired Validator validator;
    Productos product;
    Locales store;
    Precios price;
    @BeforeEach void prepare() {
        product = products.save(new Productos("Vacío de prueba"));
        store = stores.save(new Locales("Local de prueba", "San Martín 123", "-53.78", "-67.70"));
        price = new Precios(new BigDecimal("10000.01")); price.setProducto(product); price.setLocal(store);
        prices.saveAndFlush(price);
    }
    String basket() { return "{\"localId\":" + store.getId() + ",\"items\":[{\"productoId\":" + product.getId() + ",\"cantidad\":1.5}]}"; }
    @Test void publicCatalogAndAnonymousSession() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me")).andExpect(jsonPath("$.authenticated").value(false));
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("id=\"admin-form\""))))
            .andExpect(content().string(not(containsString("id=\"gestion\""))));
        mvc.perform(get("/gestion.html")).andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"admin-form\"")))
            .andExpect(content().string(not(containsString("id=\"calculadora\""))));
    }
    @Test void registrationHashesAndNeverAcceptsRole() throws Exception {
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType("application/json")
            .content("{\"email\":\"Nuevo@example.com\",\"password\":\"UnaClaveSegura123\"}"))
            .andExpect(status().isCreated());
        Usuario user = users.findByEmail("nuevo@example.com").orElseThrow();
        assertNotEquals("UnaClaveSegura123", user.getPasswordHash());
        assertTrue(encoder.matches("UnaClaveSegura123", user.getPasswordHash())); assertEquals("USER", user.getRol());
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType("application/json")
            .content("{\"email\":\"Otro@example.com\",\"password\":\"UnaClaveSegura123\",\"rol\":\"ADMIN\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void realLoginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        users.saveAndFlush(new Usuario("sesion@example.com", encoder.encode("UnaClaveSegura123"), "USER"));
        MockHttpSession before = new MockHttpSession();
        String beforeId = before.getId();
        MvcResult login = mvc.perform(post("/api/auth/login").session(before).with(csrf())
            .param("email", "sesion@example.com").param("password", "UnaClaveSegura123"))
            .andExpect(status().isNoContent()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotEquals(beforeId, session.getId());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.authenticated").value(true)).andExpect(jsonPath("$.admin").value(false));
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(jsonPath("$.authenticated").value(false));
    }
    @Test void csrfIsRequiredEvenForLoginAndPublicCalculation() throws Exception {
        mvc.perform(post("/api/auth/login").param("email", "x").param("password", "x")).andExpect(status().isForbidden());
        mvc.perform(post("/api/presupuestos/calcular").contentType("application/json").content(basket())).andExpect(status().isForbidden());
    }
    @Test void invalidCredentialsFail() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", "missing@example.com").param("password", "invalid"))
            .andExpect(status().isUnauthorized());
    }
    @Test void anonymousCannotDownloadOrManage() throws Exception {
        mvc.perform(post("/api/presupuestos/pdf").with(csrf()).contentType("application/json").content(basket())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/productos").with(csrf()).contentType("application/json").content("{\"nombre\":\"Asado\"}"))
            .andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(roles = "USER") void userCannotAdminister() throws Exception {
        mvc.perform(delete("/api/admin/productos/" + product.getId()).with(csrf())).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles = "OPERADOR") void crudAndLogicalDeletion() throws Exception {
        mvc.perform(post("/api/admin/productos").with(csrf()).contentType("application/json").content("{\"nombre\":\"Entraña\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
        mvc.perform(put("/api/admin/productos/" + product.getId()).with(csrf()).contentType("application/json").content("{\"nombre\":\"Vacío actualizado\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Vacío actualizado"));
        mvc.perform(delete("/api/admin/productos/" + product.getId()).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        assertTrue(products.existsById(product.getId())); assertFalse(products.findById(product.getId()).orElseThrow().isActivo());
        mvc.perform(get("/api/precios/ultimos")).andExpect(content().json("[]"));
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json").content(basket())).andExpect(status().isNotFound());
    }
    @Test @WithMockUser(roles = "OPERADOR") void validatesJavaFieldsAndForeignKeys() throws Exception {
        mvc.perform(post("/api/admin/productos").with(csrf()).contentType("application/json").content("{\"nombre\":\" \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.nombre").exists());
        mvc.perform(post("/api/admin/locales").with(csrf()).contentType("application/json")
            .content("{\"nombre\":\"Local\",\"direccion\":\"Calle 1\",\"latitud\":91,\"longitud\":0}"))
            .andExpect(status().isBadRequest());
        String badPrice = "{\"localId\":" + store.getId() + ",\"productoId\":" + product.getId() + ",\"precio\":-1,\"fecha\":\"" + LocalDate.now() + "\"}";
        mvc.perform(post("/api/admin/precios").with(csrf()).contentType("application/json").content(badPrice)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/precios").with(csrf()).contentType("application/json").content(badPrice.replace("\"precio\":-1", "\"precio\":12.50").replace("\"localId\":" + store.getId(), "\"localId\":999999")))
            .andExpect(status().isNotFound());
    }
    @Test @WithMockUser(roles = "OPERADOR") void editsStoresAndPricesAndPreservesLogicalDeletions() throws Exception {
        mvc.perform(put("/api/admin/locales/" + store.getId()).with(csrf()).contentType("application/json")
            .content("{\"nombre\":\"Local editado\",\"direccion\":\"Calle 456\",\"latitud\":-53.7,\"longitud\":-67.7}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.direccion").value("Calle 456"));
        String priceBody = "{\"localId\":" + store.getId() + ",\"productoId\":" + product.getId() + ",\"precio\":2500.25,\"fecha\":\"" + LocalDate.now() + "\"}";
        mvc.perform(put("/api/admin/precios/" + price.getId()).with(csrf()).contentType("application/json").content(priceBody))
            .andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(2500.25));
        mvc.perform(post("/api/admin/precios").with(csrf()).contentType("application/json")
            .content(priceBody.replace(LocalDate.now().toString(), LocalDate.now().plusDays(1).toString())))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/precios/" + price.getId()).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        assertTrue(prices.existsById(price.getId())); assertFalse(prices.findById(price.getId()).orElseThrow().isActivo());
        mvc.perform(delete("/api/admin/locales/" + store.getId()).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        assertTrue(stores.existsById(store.getId())); assertFalse(stores.findById(store.getId()).orElseThrow().isActivo());
    }
    @ParameterizedTest @ValueSource(strings = {"USER", "OPERADOR", "ADMIN"})
    void roleMatrixAppliesToEveryCatalogEntity(String role) throws Exception {
        String[] kinds = {"productos", "locales", "precios"};
        int[] ids = {product.getId(), store.getId(), price.getId()};
        String[] bodies = {
            "{\"nombre\":\"Corte editado\"}",
            "{\"nombre\":\"Local editado\",\"direccion\":\"Calle 456\",\"latitud\":-53.7,\"longitud\":-67.7}",
            "{\"localId\":" + store.getId() + ",\"productoId\":" + product.getId() + ",\"precio\":2500.25,\"fecha\":\"" + LocalDate.now() + "\"}"
        };
        for (int i = 0; i < kinds.length; i++) {
            mvc.perform(post("/api/admin/" + kinds[i]).with(user("staff").roles(role)).with(csrf())
                .contentType("application/json").content(bodies[i]))
                .andExpect(status().is(role.equals("OPERADOR") ? 201 : 403));
            mvc.perform(put("/api/admin/" + kinds[i] + "/" + ids[i]).with(user("staff").roles(role)).with(csrf())
                .contentType("application/json").content(bodies[i]))
                .andExpect(status().is(role.equals("OPERADOR") ? 200 : 403));
        }
        // Check deletions last so a parent deactivation does not affect the editing checks.
        for (int i = 0; i < kinds.length; i++) {
            mvc.perform(delete("/api/admin/" + kinds[i] + "/" + ids[i]).with(user("staff").roles(role)).with(csrf()))
                .andExpect(status().is(role.equals("ADMIN") ? 204 : 403));
        }
        assertEquals(!role.equals("ADMIN"), products.findById(product.getId()).orElseThrow().isActivo());
        assertEquals(!role.equals("ADMIN"), stores.findById(store.getId()).orElseThrow().isActivo());
        assertEquals(!role.equals("ADMIN"), prices.findById(price.getId()).orElseThrow().isActivo());
        mvc.perform(get("/api/auth/me").with(user("staff").roles(role)))
            .andExpect(jsonPath("$.admin").value(role.equals("ADMIN")))
            .andExpect(jsonPath("$.operador").value(role.equals("OPERADOR")));
    }
    @Test void bootstrapCreatesStaffWithoutChangingExistingPasswords() {
        AdminBootstrap bootstrap = new AdminBootstrap(users, encoder, validator);
        ReflectionTestUtils.setField(bootstrap, "email", "jefe@example.com");
        ReflectionTestUtils.setField(bootstrap, "password", "ClaveInicialSegura123");
        ReflectionTestUtils.setField(bootstrap, "operatorEmail", "operador@example.com");
        ReflectionTestUtils.setField(bootstrap, "operatorPassword", "ClaveOperadorSegura123");
        bootstrap.run();
        assertEquals("ADMIN", users.findByEmail("jefe@example.com").orElseThrow().getRol());
        Usuario operator = users.findByEmail("operador@example.com").orElseThrow();
        assertEquals("OPERADOR", operator.getRol());
        assertTrue(encoder.matches("ClaveOperadorSegura123", operator.getPasswordHash()));
        ReflectionTestUtils.setField(bootstrap, "operatorPassword", "OtraClaveDiferente123");
        bootstrap.run();
        assertEquals(operator.getPasswordHash(), users.findByEmail("operador@example.com").orElseThrow().getPasswordHash());
        ReflectionTestUtils.setField(bootstrap, "operatorEmail", "\"operador@example.com\"");
        assertThrows(IllegalStateException.class, bootstrap::run);
    }
    @Test void calculatesDecimalMoneyAndLatestPriceDeterministically() throws Exception {
        Precios old = new Precios(new BigDecimal("1.00")); old.setFecha(LocalDate.now().minusDays(1)); old.setLocal(store); old.setProducto(product); prices.saveAndFlush(old);
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json").content(basket()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(15000.02));
        Precios newer = new Precios(new BigDecimal("12000.00")); newer.setLocal(store); newer.setProducto(product); prices.saveAndFlush(newer);
        mvc.perform(get("/api/precios/ultimos")).andExpect(jsonPath("$[0].productos.length()").value(1)).andExpect(jsonPath("$[0].productos[0].precio").value(12000.00));
        newer.setActivo(false); prices.saveAndFlush(newer);
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json").content(basket()))
            .andExpect(jsonPath("$.total").value(15000.02));
    }
    @Test void rejectsMissingPricesDuplicateItemsAndInvalidAmounts() throws Exception {
        price.setActivo(false); prices.saveAndFlush(price);
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json").content(basket())).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json").content(basket().replace("1.5", "-2"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json").content(basket().replace("1.5", "1.2345"))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings = {"0.5", "1", "1.5", "1.50", "1000"})
    void acceptsOnlyHalfKiloSteps(String quantity) throws Exception {
        BigDecimal expected = new BigDecimal("10000.01").multiply(new BigDecimal(quantity)).setScale(2, java.math.RoundingMode.HALF_UP);
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json")
            .content(basket().replace("1.5", quantity)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(expected.doubleValue()));
    }
    @ParameterizedTest @ValueSource(strings = {"0", "0.25", "0.75", "1.2", "1.2345", "-0.5", "1000.5"})
    void rejectsOtherQuantitiesInCalculationAndPdf(String quantity) throws Exception {
        for (String endpoint : List.of("calcular", "pdf")) {
            mvc.perform(post("/api/presupuestos/" + endpoint).with(user("cliente").roles("USER")).with(csrf())
                .contentType("application/json").content(basket().replace("1.5", quantity)))
                .andExpect(status().isBadRequest());
        }
    }
    @Test void rejectsDuplicatedBasketItems() throws Exception {
        String item = "{\"productoId\":" + product.getId() + ",\"cantidad\":1}";
        mvc.perform(post("/api/presupuestos/calcular").with(csrf()).contentType("application/json")
            .content("{\"localId\":" + store.getId() + ",\"items\":[" + item + "," + item + "]}"))
            .andExpect(status().isBadRequest());
    }
    @Test @WithMockUser void authenticatedPdfContainsCalculatedTotal() throws Exception {
        byte[] bytes = mvc.perform(post("/api/presupuestos/pdf").with(csrf()).contentType("application/json").content(basket()))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse().getContentAsByteArray();
        try (PDDocument document = PDDocument.load(bytes)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Vacío de prueba")); assertTrue(text.contains("15.000,02")); assertTrue(text.contains("TOTAL"));
        }
    }
    @Test void pdfEmbedsFontsAndWrapsLongNamesWithoutLosingAmounts() throws Exception {
        String name = "CorteEspecialSinEspacios".repeat(5);
        var lines = List.of(new Presupuesto.Linea(1, name, new BigDecimal("1000"),
            new BigDecimal("9999999999.99"), new BigDecimal("9999999999990.00"), LocalDate.now()));
        byte[] bytes = pdf.generate(new Presupuesto(1, "Comercio con nombre largo ".repeat(4), "Dirección extensa ".repeat(8), lines,
            new BigDecimal("9999999999990.00")));
        try (PDDocument document = PDDocument.load(bytes)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.replaceAll("\\s+", "").contains(name));
            assertTrue(text.contains("9.999.999.999.990,00"));
            for (var page : document.getPages())
                for (var fontName : page.getResources().getFontNames())
                    assertTrue(page.getResources().getFont(fontName).isEmbedded());
        }
    }
    @Test void pdfPaginatesLargeBudgets() throws Exception {
        List<Presupuesto.Linea> lines = new ArrayList<>();
        for (int i = 0; i < 30; i++) lines.add(new Presupuesto.Linea(i, "Corte " + i, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, LocalDate.now()));
        try (PDDocument document = PDDocument.load(pdf.generate(new Presupuesto(1, "Local", "Dirección", lines, new BigDecimal("300.00"))))) {
            assertTrue(document.getNumberOfPages() >= 3);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Corte 29"));
            assertTrue(text.contains("Página 1 de " + document.getNumberOfPages()));
            assertTrue(text.contains("Página " + document.getNumberOfPages() + " de " + document.getNumberOfPages()));
            assertTrue(text.contains("300,00"));
        }
    }
}
