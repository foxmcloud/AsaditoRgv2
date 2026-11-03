package com.appasadito.AsaditoRg.controllers;
import com.appasadito.AsaditoRg.dtos.*;
import com.appasadito.AsaditoRg.services.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.io.IOException;
@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {
    private final PresupuestoService calculator;
    private final PdfService pdf;
    public PresupuestoController(PresupuestoService calculator, PdfService pdf) { this.calculator = calculator; this.pdf = pdf; }
    @PostMapping("/calcular")
    public Presupuesto calculate(@Valid @RequestBody PresupuestoRequest request) { return calculator.calculate(request); }
    @PostMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@Valid @RequestBody PresupuestoRequest request) throws IOException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=presupuesto-asadito.pdf")
            .body(pdf.generate(calculator.calculate(request)));
    }
}
