package com.appasadito.AsaditoRg.services;
import com.appasadito.AsaditoRg.dtos.Presupuesto;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Service;
import java.io.*;
import java.text.NumberFormat;
import java.util.Locale;
@Service
public class PdfService {
    public byte[] generate(Presupuesto quote) throws IOException {
        NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            PDPageContentStream content = null;
            try {
                int row = 0;
                for (int index = 0; index <= quote.items.size(); index++) {
                    if (row == 0 || row >= 14) {
                        if (content != null) content.close();
                        PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        text(content, 20, 790, "ASADITO RG | Presupuesto");
                        text(content, 11, 765, quote.localNombre);
                        text(content, 10, 747, quote.direccion);
                        text(content, 10, 725, "Generado: " + quote.generado + " | Moneda: ARS | Pag. " + document.getNumberOfPages());
                        row = 1;
                    }
                    float y = 710 - row * 40;
                    if (index < quote.items.size()) {
                        Presupuesto.Linea line = quote.items.get(index);
                        text(content, 11, y, line.nombre);
                        text(content, 10, y - 16, line.cantidad.toPlainString() + " kg x " + money.format(line.precio)
                            + " = " + money.format(line.subtotal) + " | Precio del " + line.fecha);
                    } else {
                        text(content, 16, y, "TOTAL: " + money.format(quote.total));
                        text(content, 9, y - 22, "Estimativo. Verificá vigencia y disponibilidad con el comercio.");
                    }
                    row++;
                }
            } finally { if (content != null) content.close(); }
            document.save(result); return result.toByteArray();
        }
    }
    private void text(PDPageContentStream stream, float size, float y, String value) throws IOException {
        StringBuilder safe = new StringBuilder();
        for (int cp : value.codePoints().toArray()) {
            String letter = new String(Character.toChars(cp));
            try { if (Character.isISOControl(cp)) letter = " "; PDType1Font.HELVETICA.encode(letter); }
            catch (IllegalArgumentException ex) { letter = "?"; }
            safe.append(letter);
        }
        String rendered = safe.toString();
        while (PDType1Font.HELVETICA.getStringWidth(rendered) / 1000 * size > 495)
            rendered = rendered.substring(0, rendered.length() - 1);
        stream.beginText(); stream.setFont(PDType1Font.HELVETICA, size); stream.newLineAtOffset(50, y);
        stream.showText(rendered); stream.endText();
    }
}
