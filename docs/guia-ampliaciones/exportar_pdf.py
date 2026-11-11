"""Exporta la guía Markdown a PDF. Requiere reportlab y fuentes DejaVu.
Ejecutar desde el repositorio: python3 docs/guia-ampliaciones/exportar_pdf.py
No modifica el código de la aplicación.
"""
from pathlib import Path
import re
from xml.sax.saxutils import escape
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle,
    Flowable, XPreformatted,
)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.utils import ImageReader

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'docs/GUIA_AMPLIACIONES_BACKEND.md'
OUTPUT = SOURCE.with_suffix('.pdf')
FONT_DIR = Path('/usr/share/fonts/truetype/dejavu')
for name, filename in [('Texto', 'DejaVuSans.ttf'), ('TextoBold', 'DejaVuSans-Bold.ttf'),
                       ('Codigo', 'DejaVuSansMono.ttf')]:
    pdfmetrics.registerFont(TTFont(name, str(FONT_DIR / filename)))
pdfmetrics.registerFontFamily('Texto', normal='Texto', bold='TextoBold', italic='Texto', boldItalic='TextoBold')
GREEN = colors.HexColor('#335c48')
ORANGE = colors.HexColor('#a94024')
INK = colors.HexColor('#292a24')
GREY = colors.HexColor('#64675e')
CREAM = colors.HexColor('#faf7f0')
LINE = colors.HexColor('#dedfd5')
WIDTH, HEIGHT = A4
INNER = WIDTH - 100
styles = {
    'body': ParagraphStyle('Body', fontName='Texto', fontSize=9.2, leading=14,
                           textColor=INK, spaceAfter=8),
    'h1': ParagraphStyle('Chapter', fontName='TextoBold', fontSize=20, leading=27,
                         textColor=GREEN, spaceAfter=15, keepWithNext=True),
    'h2': ParagraphStyle('Subheading', fontName='TextoBold', fontSize=12, leading=17,
                         textColor=ORANGE, spaceBefore=13, spaceAfter=8, keepWithNext=True),
    'cell': ParagraphStyle('Cell', fontName='Texto', fontSize=8, leading=11.8, textColor=INK),
    'th': ParagraphStyle('TableHeader', fontName='TextoBold', fontSize=8, leading=11.8,
                         textColor=colors.white),
    'code': ParagraphStyle('Code', fontName='Codigo', fontSize=7.5, leading=10.8,
                           textColor=INK, backColor=CREAM, borderColor=LINE,
                           borderWidth=.3, borderPadding=8, spaceBefore=7, spaceAfter=15),
    'toc': ParagraphStyle('Contents', fontName='Texto', fontSize=10, leading=18,
                          textColor=INK, spaceBefore=9, leftIndent=0, firstLineIndent=0),
}


def inline(text):
    text = escape(text)
    text = re.sub(r'`([^`]+)`', r'<font name="Codigo">\1</font>', text)
    text = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', text)
    text = re.sub(r'(https://[^\s<>]+)',
                  lambda m: m.group(1) if m.group(1) == 'https://tu-dominio' else '<link href="' + m.group(1) + '" color="#335c48">' + m.group(1) + '</link>', text)
    return text


class GuideDoc(SimpleDocTemplate):
    def afterFlowable(self, flowable):
        if hasattr(flowable, 'toc_text'):
            self.canv.bookmarkPage(flowable.anchor)
            self.canv.addOutlineEntry(flowable.toc_text, flowable.anchor, 0)
            self.notify('TOCEntry', (0, flowable.toc_text, self.page, flowable.anchor))


def heading(title, key):
    p = Paragraph(inline(title), styles['h1'])
    p.toc_text, p.anchor = title, key
    return p


class DataMap(Flowable):
    def __init__(self):
        super().__init__()
        self.width, self.height = INNER, 414

    def draw(self):
        c = self.canv
        def connection(points, label, x, y):
            c.setStrokeColor(GREY)
            c.setLineWidth(.8)
            p = c.beginPath(); p.moveTo(*points[0])
            for point in points[1:]: p.lineTo(*point)
            c.drawPath(p)
            c.setFillColor(GREEN); c.setFont('Texto', 7.5)
            c.drawString(x, y, label)
        connection([(110, 307), (345, 307)], '1 a 0..1', 207, 315)
        connection([(110, 312), (165, 375)], '1 a N', 122, 350)
        connection([(110, 287), (165, 237)], '1 a N', 119, 266)
        connection([(110, 146), (165, 212)], '1 a N', 120, 174)
        connection([(240, 195), (240, 125)], '1 a N', 247, 158)
        connection([(315, 225), (345, 225)], '1 a 0..1', 315, 265)
        connection([(110, 41), (165, 91)], '1 a N', 116, 72)
        connection([(315, 95), (345, 95)], 'N a 1', 316, 135)
        boxes = [
            (0, 280, 110, 52, 'Usuario', 'existente'),
            (165, 350, 150, 52, 'Movimiento', 'quién hizo qué'),
            (345, 280, 150, 52, 'ConfirmacionEmail', 'token y vencimiento'),
            (165, 195, 150, 60, 'PresupuestoGuardado', 'dueño, local y total'),
            (0, 120, 110, 52, 'Locales', 'existente'),
            (345, 195, 150, 60, 'Verificacion', 'estimado vs. pagado'),
            (165, 65, 150, 60, 'DetallePresupuesto', 'corte, kilos y precio'),
            (0, 15, 110, 52, 'Productos', 'existente'),
            (345, 65, 150, 60, 'Precios', 'existente / origen'),
        ]
        for x, y, w, h, title, subtitle in boxes:
            c.setFillColor(CREAM); c.setStrokeColor(LINE)
            c.roundRect(x, y, w, h, 7, stroke=1, fill=1)
            c.setFillColor(GREEN); c.setFont('TextoBold', 8.2 if len(title)>19 else 9)
            c.drawCentredString(x + w/2, y+h-21, title)
            c.setFillColor(GREY); c.setFont('Texto', 8)
            c.drawCentredString(x+w/2, y+13, subtitle)


def page(canvas, doc):
    canvas.saveState()
    if doc.page == 1:
        canvas.setFillColor(CREAM); canvas.rect(0, 0, WIDTH, HEIGHT, fill=1, stroke=0)
        canvas.setFillColor(ORANGE); canvas.rect(0, HEIGHT-7, WIDTH, 7, fill=1, stroke=0)
        logo = ROOT/'src/main/resources/static/assets/img/Logo Asadito RG - PNG.png'
        canvas.drawImage(ImageReader(str(logo)), 28, HEIGHT-126, width=260,
                         height=260*897/2481, mask='auto')
        canvas.setFont('TextoBold', 9); canvas.setFillColor(ORANGE)
        canvas.drawString(50, 646, 'GUÍA DE IMPLEMENTACIÓN MANUAL')
        canvas.setFont('TextoBold', 34); canvas.setFillColor(INK)
        canvas.drawString(50, 586, 'Ampliaciones')
        canvas.drawString(50, 542, 'del backend')
        p = Paragraph('Movimientos, presupuestos guardados y verificaciones.<br/>'
                      'Confirmación de emails y chatbot de WhatsApp opcional.',
                      ParagraphStyle('CoverSubtitle', fontName='Texto', fontSize=13,
                                     leading=21, textColor=GREY))
        p.wrapOn(canvas, INNER, 100); p.drawOn(canvas, 50, 450)
        canvas.setFillColor(colors.white); canvas.roundRect(50, 253, INNER, 132, 10, fill=1, stroke=0)
        p = Paragraph('<b>Tu punto de partida</b><br/>'
                      'Java 11 · Spring Boot 2.7.14 · PostgreSQL<br/>'
                      'Sesiones actuales y DTO originales.<br/><br/>'
                      'Una etapa por vez: tablas, relaciones, código clave, permisos y pruebas.',
                      ParagraphStyle('CoverCard', fontName='Texto', fontSize=10,
                                     leading=16, textColor=INK))
        p.wrapOn(canvas, INNER-38, 115); p.drawOn(canvas, 69, 273)
        canvas.setFillColor(GREEN); canvas.setFont('TextoBold', 10)
        canvas.drawString(50, 197, 'ASADITO RG')
        canvas.setFont('Texto', 9); canvas.setFillColor(GREY)
        canvas.drawString(50, 175, '11 de noviembre de 2026 · Base de código: 4d58c55')
        canvas.drawString(50, 87, 'Propuesta para codificar a mano. La aplicación no fue modificada.')
    else:
        canvas.setFont('TextoBold', 7.5); canvas.setFillColor(GREEN)
        canvas.drawString(50, HEIGHT-31, 'ASADITO RG · AMPLIACIONES DEL BACKEND')
        canvas.setFont('Texto', 7.5); canvas.setFillColor(GREY)
        canvas.drawRightString(WIDTH-50, HEIGHT-31, 'Guía de implementación manual')
        canvas.setStrokeColor(LINE); canvas.line(50, HEIGHT-40, WIDTH-50, HEIGHT-40)
    canvas.setStrokeColor(LINE); canvas.line(50, 46, WIDTH-50, 46)
    canvas.setFont('Texto', 7.5); canvas.setFillColor(GREY)
    canvas.drawString(50, 30, 'Movimientos · Presupuestos · Verificaciones · Email · WhatsApp')
    canvas.drawRightString(WIDTH-50, 30, str(doc.page))
    canvas.restoreState()


story = [Spacer(1, 1), PageBreak(), Paragraph('Contenido', styles['h1'])]
story.append(Paragraph('Usá el índice para avanzar por etapas. Los enlaces internos también funcionan en el PDF.', styles['body']))
story.append(Paragraph('Propuesta adaptada al commit 4d58c55. Los fragmentos se completan a mano; las funciones descriptas todavía no se agregaron a la aplicación.', styles['body']))
toc = TableOfContents(); toc.levelStyles = [styles['toc']]
story.extend([toc, PageBreak(), heading('Mapa de relaciones', 'mapa'),
              Paragraph('Las líneas muestran las relaciones nuevas. “1 a N” representa uno a muchos; “1 a 0..1”, una relación opcional de uno a uno.', styles['body']),
              DataMap(), Spacer(1, 10),
              Paragraph('Precios ya relaciona Locales y Productos en tu código original; esas relaciones no se vuelven a dibujar. El detalle conserva su precio de origen y una copia de los valores usados.', styles['body']),
              Paragraph('El vínculo al usuario identifica al dueño. El presupuesto guardado tiene detalles; su verificación compara el total histórico con un pago declarado. La confirmación de email pertenece a la cuenta, no al presupuesto.', styles['body'])])
lines = SOURCE.read_text().splitlines(); i = next(n for n, line in enumerate(lines) if line.startswith('## ')); chapter = 0
while i < len(lines):
    line = lines[i]
    if line.startswith('# '): i += 1; continue
    if not line.strip(): i += 1; continue
    if line.startswith('```'):
        block = []; i += 1
        while i < len(lines) and not lines[i].startswith('```'):
            block.append(lines[i]); i += 1
        story.append(XPreformatted(escape('\n'.join(block)), styles['code'])); i += 1; continue
    if line.startswith('|'):
        rows=[]
        while i < len(lines) and lines[i].startswith('|'):
            cells=[x.strip() for x in lines[i].strip().strip('|').split('|')]
            if not all(re.fullmatch(r'[-: ]+', x) for x in cells): rows.append(cells)
            i += 1
        columns = len(rows[0]); first = rows[0][0]
        if columns == 5: widths = [72, 72, 76, 75, INNER-295]
        elif columns == 3 and first == 'Orden': widths = [36, 150, INNER-186]
        elif columns == 3 and first.startswith('Método'): widths = [223, 108, INNER-331]
        elif columns == 3 and first == 'Etapa': widths = [70, 215, INNER-285]
        elif columns == 3: widths = [110, 160, INNER-270]
        else: widths = [132, INNER-132]
        formatted = [[Paragraph(inline(cell), styles['th' if n==0 else 'cell']) for cell in row]
                     for n,row in enumerate(rows)]
        table=Table(formatted, colWidths=widths, repeatRows=1, hAlign='LEFT')
        table.setStyle(TableStyle([
            ('BACKGROUND',(0,0),(-1,0), GREEN),
            ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,CREAM]),
            ('GRID',(0,0),(-1,-1),.35,LINE),('VALIGN',(0,0),(-1,-1),'TOP'),
            ('LEFTPADDING',(0,0),(-1,-1),8),('RIGHTPADDING',(0,0),(-1,-1),8),
            ('TOPPADDING',(0,0),(-1,-1),8),('BOTTOMPADDING',(0,0),(-1,-1),8),
        ]))
        story.extend([Spacer(1,5),table,Spacer(1,12)]); continue
    if line.startswith('## '):
        chapter += 1; story.extend([PageBreak(),heading(line[3:], f'capitulo-{chapter}')])
    elif line.startswith('### '):
        story.append(Paragraph(inline(line[4:]), styles['h2']))
    else:
        if line.startswith('- '): line = '• '+line[2:]
        story.append(Paragraph(inline(line), styles['body']))
    i += 1

doc=GuideDoc(str(OUTPUT),pagesize=A4,leftMargin=50,rightMargin=50,topMargin=60,
             bottomMargin=62,title='Asadito RG - Guía para ampliar el backend a mano',
             author='Asadito RG',subject='Movimientos, presupuestos, verificaciones, email y WhatsApp')
doc.multiBuild(story,onFirstPage=page,onLaterPages=page)
print(OUTPUT)
