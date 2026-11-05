package com.appasadito.AsaditoRg.services;

import com.appasadito.AsaditoRg.dtos.Presupuesto;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PdfService {
    public byte[] generate(Presupuesto quote) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.getDocumentInformation().setTitle("Presupuesto de asado - " + quote.localNombre);
            document.getDocumentInformation().setAuthor("Asadito RG");
            document.getDocumentInformation().setSubject("Lista de cortes, cantidades y precios estimados en ARS");
            try (Layout layout = new Layout(document, quote)) {
                layout.render();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    /** Coordinates, wrapping and page breaks live here; monetary calculations stay in PresupuestoService. */
    private static final class Layout implements AutoCloseable {
        private static final float WIDTH = PDRectangle.A4.getWidth();
        private static final float HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 44;
        private static final float CONTENT = WIDTH - MARGIN * 2;
        private static final float BOTTOM = 85;
        private static final Color INK = new Color(41, 42, 36);
        private static final Color MUTED = new Color(100, 103, 94);
        private static final Color ORANGE = new Color(169, 64, 36);
        private static final Color GREEN = new Color(51, 92, 72);
        private static final Color CREAM = new Color(250, 247, 240);
        private static final Color LINE = new Color(228, 223, 210);
        private final PDFont REGULAR;
        private final PDFont BOLD;
        private final PDFont TITLE;
        private final PDImageXObject logo;
        private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private final PDDocument document;
        private final Presupuesto quote;
        private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        private PDPageContentStream stream;
        private float y;

        Layout(PDDocument document, Presupuesto quote) throws IOException {
            this.document = document; this.quote = quote;
            REGULAR = font("LiberationSans-Regular.ttf");
            BOLD = font("LiberationSans-Bold.ttf");
            TITLE = font("LiberationSerif-Regular.ttf");
            try (InputStream image = PdfService.class.getResourceAsStream("/static/assets/img/Logo Asadito RG - PNG.png")) {
                if (image == null) throw new IOException("No se encontró el logo de Asadito RG");
                logo = PDImageXObject.createFromByteArray(document, image.readAllBytes(), "asadito-rg-logo");
            }
        }
        private PDFont font(String name) throws IOException {
            try (InputStream source = PdfService.class.getResourceAsStream("/fonts/" + name)) {
                if (source == null) throw new IOException("No se encontró la fuente PDF " + name);
                return PDType0Font.load(document, source, true);
            }
        }

        void render() throws IOException {
            startPage(false);
            tableHeader();
            for (int index = 0; index < quote.items.size(); index++) {
                Presupuesto.Linea item = quote.items.get(index);
                List<String> names = wrap(item.nombre, BOLD, 10, 178);
                float height = Math.max(55, 31 + names.size() * 13);
                if (y - height < BOTTOM) {
                    startPage(true);
                    tableHeader();
                }
                row(item, names, height, index % 2 == 0);
            }
            if (y - 163 < BOTTOM) startPage(true);
            totals();
            close();
            footers();
        }

        private void startPage(boolean continuation) throws IOException {
            close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            rect(0, HEIGHT - 187, WIDTH, 187, CREAM);
            rect(0, HEIGHT - 6, WIDTH, 6, ORANGE);
            // Source artwork includes transparent margins. Place it without distorting the image.
            float logoWidth = 234;
            float logoHeight = logoWidth * logo.getHeight() / logo.getWidth();
            stream.drawImage(logo, MARGIN - 26, HEIGHT - 96, logoWidth, logoHeight);
            text("LA JUNTADA EMPIEZA ACÁ", MARGIN, HEIGHT - 86, BOLD, 7.3f, MUTED);
            right("Río Grande · Tierra del Fuego", WIDTH - MARGIN, HEIGHT - 53, REGULAR, 9, MUTED);
            right("PRESUPUESTO ESTIMATIVO", WIDTH - MARGIN, HEIGHT - 72, BOLD, 7.5f, ORANGE);
            text(continuation ? "Tu presupuesto, en detalle." : "Tu asado, en números.", MARGIN,
                HEIGHT - 122, TITLE, continuation ? 29 : 34, INK);
            text("Generado el " + DATE.format(quote.generado) + "   /   Moneda: ARS · Pesos argentinos",
                MARGIN, HEIGHT - 145, REGULAR, 9, MUTED);
            y = HEIGHT - 175;
            storeCard();
        }

        private void storeCard() throws IOException {
            List<String> name = wrap(quote.localNombre, BOLD, 13, CONTENT - 34);
            List<String> address = wrap(quote.direccion, REGULAR, 9, CONTENT - 34);
            float height = 45 + name.size() * 16 + address.size() * 12;
            rounded(MARGIN, y - height, CONTENT, height, 9, Color.WHITE);
            stream.setStrokingColor(LINE); stream.setLineWidth(.7f);
            stream.addRect(MARGIN, y - height, CONTENT, height); stream.stroke();
            text("TU COMERCIO", MARGIN + 16, y - 20, BOLD, 7, ORANGE);
            float baseline = y - 40;
            for (String line : name) { text(line, MARGIN + 16, baseline, BOLD, 13, INK); baseline -= 16; }
            for (String line : address) { text(line, MARGIN + 16, baseline - 1, REGULAR, 9, MUTED); baseline -= 12; }
            y -= height + 24;
        }

        private void tableHeader() throws IOException {
            rounded(MARGIN, y - 30, CONTENT, 30, 5, GREEN);
            text("CORTE / FECHA DEL PRECIO", MARGIN + 12, y - 19, BOLD, 7.5f, Color.WHITE);
            right("KG", MARGIN + 246, y - 19, BOLD, 8, Color.WHITE);
            right("PRECIO / KG", MARGIN + 358, y - 19, BOLD, 8, Color.WHITE);
            right("SUBTOTAL", WIDTH - MARGIN - 12, y - 19, BOLD, 8, Color.WHITE);
            y -= 30;
        }

        private void row(Presupuesto.Linea item, List<String> names, float height, boolean shaded) throws IOException {
            if (shaded) rect(MARGIN, y - height, CONTENT, height, CREAM);
            float baseline = y - 20;
            for (String name : names) { text(name, MARGIN + 12, baseline, BOLD, 10, INK); baseline -= 13; }
            text("Precio del " + DATE.format(item.fecha), MARGIN + 12, baseline - 2, REGULAR, 8, MUTED);
            right(item.cantidad.stripTrailingZeros().toPlainString().replace('.', ','), MARGIN + 246, y - 22, REGULAR, 10, INK);
            right(money.format(item.precio), MARGIN + 358, y - 22, REGULAR, 9.5f, INK);
            right(money.format(item.subtotal), WIDTH - MARGIN - 12, y - 22, BOLD, 9.5f, INK);
            stream.setStrokingColor(LINE); stream.setLineWidth(.5f);
            stream.moveTo(MARGIN, y - height); stream.lineTo(WIDTH - MARGIN, y - height); stream.stroke();
            y -= height;
        }

        private void totals() throws IOException {
            y -= 20;
            BigDecimal kilos = quote.items.stream().map(item -> item.cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
            rounded(MARGIN, y - 81, CONTENT, 81, 9, GREEN);
            text("TOTAL ESTIMADO", MARGIN + 17, y - 25, BOLD, 9, Color.WHITE);
            text(quote.items.size() + " cortes · " + kilos.stripTrailingZeros().toPlainString().replace('.', ',') + " kg en total",
                MARGIN + 17, y - 45, REGULAR, 9, Color.WHITE);
            text("Pesos argentinos · ARS", MARGIN + 17, y - 64, REGULAR, 8, new Color(218, 231, 215));
            String total = money.format(quote.total);
            float size = 25;
            while (width(total, BOLD, size) > 265 && size > 12) size -= .5f;
            right(total, WIDTH - MARGIN - 17, y - 47, BOLD, size, Color.WHITE);
            y -= 102;
            text("ANTES DE ENCENDER EL FUEGO", MARGIN, y, BOLD, 8, ORANGE);
            text("Confirmá vigencia y disponibilidad con el comercio. Los importes no incluyen envío.",
                MARGIN, y - 17, REGULAR, 8.5f, MUTED);
            text("Este presupuesto es orientativo y no constituye una reserva ni un comprobante de compra.",
                MARGIN, y - 31, REGULAR, 8.5f, MUTED);
        }

        private void footers() throws IOException {
            int number = 1;
            for (PDPage page : document.getPages()) {
                try (PDPageContentStream footer = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    stream = footer;
                    stream.setStrokingColor(LINE); stream.setLineWidth(.6f);
                    stream.moveTo(MARGIN, 57); stream.lineTo(WIDTH - MARGIN, 57); stream.stroke();
                    stream.drawImage(logo, MARGIN - 11, 22, 100, 100f * logo.getHeight() / logo.getWidth());
                    text("Compará. Calculá. Compartí.", MARGIN + 100, 39, REGULAR, 8, MUTED);
                    right("Página " + number++ + " de " + document.getNumberOfPages(), WIDTH - MARGIN, 39, REGULAR, 8, MUTED);
                } finally { stream = null; }
            }
        }

        private void text(String value, float x, float baseline, PDFont font, float size, Color color) throws IOException {
            stream.beginText(); stream.setFont(font, size); stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline); stream.showText(safe(value, font)); stream.endText();
        }
        private void right(String value, float right, float baseline, PDFont font, float size, Color color) throws IOException {
            text(value, right - width(value, font, size), baseline, font, size, color);
        }
        private float width(String value, PDFont font, float size) throws IOException {
            return font.getStringWidth(safe(value, font)) / 1000 * size;
        }
        private String safe(String value, PDFont font) throws IOException {
            StringBuilder result = new StringBuilder();
            for (int cp : value.codePoints().toArray()) {
                String letter = Character.isISOControl(cp) ? " " : new String(Character.toChars(cp));
                try { font.encode(letter); } catch (IllegalArgumentException ex) { letter = "?"; }
                result.append(letter);
            }
            return result.toString();
        }
        private List<String> wrap(String value, PDFont font, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String current = "";
            for (String word : safe(value, font).split("\\s+")) {
                if (!current.isEmpty() && width(current + " " + word, font, size) > maxWidth) {
                    lines.add(current); current = "";
                }
                // Split even a single long word so no name is silently truncated.
                for (int i = 0; i < word.length(); i++) {
                    String next = current + (i == 0 && !current.isEmpty() ? " " : "") + word.charAt(i);
                    if (width(next, font, size) > maxWidth) { lines.add(current); current = "" + word.charAt(i); }
                    else current = next;
                }
            }
            if (!current.isEmpty()) lines.add(current);
            return lines.isEmpty() ? List.of("") : lines;
        }
        private void rect(float x, float bottom, float w, float h, Color color) throws IOException {
            stream.setNonStrokingColor(color); stream.addRect(x, bottom, w, h); stream.fill();
        }
        private void rounded(float x, float bottom, float w, float h, float radius, Color color) throws IOException {
            float k = radius * .55228475f;
            stream.setNonStrokingColor(color);
            stream.moveTo(x + radius, bottom); stream.lineTo(x + w - radius, bottom);
            stream.curveTo(x + w - radius + k, bottom, x + w, bottom + radius - k, x + w, bottom + radius);
            stream.lineTo(x + w, bottom + h - radius);
            stream.curveTo(x + w, bottom + h - radius + k, x + w - radius + k, bottom + h, x + w - radius, bottom + h);
            stream.lineTo(x + radius, bottom + h);
            stream.curveTo(x + radius - k, bottom + h, x, bottom + h - radius + k, x, bottom + h - radius);
            stream.lineTo(x, bottom + radius);
            stream.curveTo(x, bottom + radius - k, x + radius - k, bottom, x + radius, bottom);
            stream.closePath(); stream.fill();
        }
        @Override public void close() throws IOException { if (stream != null) { stream.close(); stream = null; } }
    }
}
