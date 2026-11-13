// Prueba del frontend con el contrato JSON compartido por el usuario.
// Requiere Playwright y Chromium. No crea datos ni modifica el backend.
const { chromium } = require('playwright');
const { createServer } = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const { spawnSync } = require('node:child_process');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../../main/resources/static');
const output = process.env.ASADITO_PREVIEW_DIR || fs.mkdtempSync(path.join(os.tmpdir(), 'asadito-history-'));
fs.mkdirSync(output, { recursive: true });
const fixtures = [
  { id: 41, fecha: '2026-11-13T01:30:00Z', localNombre: 'Carnicería La Costa', localDireccion: 'Calle de prueba 123', moneda: 'ARS', total: '17777.77', detalles: [{ id: 90, corteNombre: 'Vacío histórico', cantidad: '1.5', precioKg: '12000', subtotal: '17777.77' }] },
  { id: 42, fecha: '2026-11-13', localNombre: 'Mercado del Sur', localDireccion: 'Avenida de prueba 456', moneda: 'ARS', total: 25000, detalles: [{ id: 91, corteNombre: 'Asado', cantidad: 1, precioKg: 25000, subtotal: 25000 }] },
  { id: 43, fecha: '2026-11-14', localNombre: '<img src=x onerror=alert(1)>', localDireccion: 'Dirección <de prueba>', moneda: 'ARS', total: 30000, detalles: [{ id: 92, corteNombre: '<script>window.inyectado=true</script>', cantidad: 2, precioKg: 15000, subtotal: 30000 }] },
];
const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.png': 'image/png', '.ttf': 'font/ttf' };
const server = createServer((req, res) => {
  const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  const file = path.resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname));
  if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404); res.end(); return; }
  res.setHeader('Content-Type', types[path.extname(file)] || 'application/octet-stream');
  fs.createReadStream(file).pipe(res);
});
let browser;
(async () => {
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(0, '127.0.0.1', resolve); });
  const origin = `http://127.0.0.1:${server.address().port}`;
  const executablePath = process.env.CHROMIUM_PATH || (fs.existsSync('/usr/bin/chromium') ? '/usr/bin/chromium' : undefined);
  browser = await chromium.launch({ executablePath, headless: true, args: ['--no-sandbox'] });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } });
  const errors = [], calls = [], downloads = [];
  let authenticated = true, mode = 'array', failDetail = false, delayed = false;
  page.on('pageerror', error => errors.push(error.message));
  page.on('download', download => downloads.push(download));
  await page.route('**/api/**', async route => {
    const request = route.request(), url = new URL(request.url());
    calls.push({ path: url.pathname, method: request.method() });
    const json = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    if (url.pathname === '/api/auth/csrf') return json({ token: 'csrf-prueba', headerName: 'X-CSRF-TOKEN' });
    if (url.pathname === '/api/auth/me') return json({ authenticated, email: authenticated ? 'prueba@example.com' : '', admin: false, operador: false });
    if (url.pathname === '/api/logout') { authenticated = false; return route.fulfill({ status: 204 }); }
    if (url.pathname === '/api/login') {
      assert.equal(new URLSearchParams(request.postData()).get('email'), 'prueba@example.com');
      assert.equal(request.headers()['x-csrf-token'], 'csrf-prueba');
      authenticated = true; return route.fulfill({ status: 204 });
    }
    if (url.pathname.startsWith('/api/presupuestos') && !authenticated) return json({ message: 'Sesión requerida' }, 401);
    if (url.pathname === '/api/presupuestos') {
      if (mode === 'error') return json({ message: 'Error de prueba' }, 500);
      if (mode === 'empty') return json([]);
      if (mode === 'page') {
        const n = Number(url.searchParams.get('page') || 0);
        return json({ content: n ? [fixtures[2]] : fixtures.slice(0, 2), number: n, totalPages: 2 });
      }
      return json(fixtures);
    }
    if (url.pathname.endsWith('/verificacion')) return json({ message: 'Todavía no tiene verificación' }, 404);
    if (url.pathname.startsWith('/api/presupuestos/')) {
      if (delayed) await new Promise(resolve => setTimeout(resolve, 180));
      if (failDetail) return json({ message: 'Presupuesto inexistente' }, 404);
      return json(fixtures.find(p => p.id === Number(url.pathname.split('/').pop())));
    }
    return json({ message: 'Endpoint no previsto en esta prueba' }, 404);
  });
  await page.goto(origin + '/mis-presupuestos.html');
  await page.locator('.saved-budget').nth(2).waitFor();
  assert.equal(await page.locator('#listado img').count(), 0);
  assert.ok(await page.locator('.brand-logo').first().isVisible());
  await page.locator('#buscar').fill('costa');
  assert.equal(await page.locator('.saved-budget').count(), 1);
  await page.locator('#buscar').fill('sin coincidencias');
  await page.getByRole('button', { name: 'Limpiar búsqueda' }).click();
  await page.locator('#orden').selectOption('menor');
  assert.match(await page.locator('.saved-budget').first().innerText(), /La Costa/);
  await page.screenshot({ path: path.join(output, 'historial-desktop.png'), fullPage: true });
  await page.locator('[data-ver="41"]').click();
  await page.locator('.saved-detail-items').waitFor();
  assert.equal(await page.locator('.saved-detail-items li').count(), 1);
  assert.match(await page.locator('.saved-detail-total').innerText(), /17.777,77/);
  assert.equal(await page.locator('#detalle-dialogo table').count(), 0);
  const download = page.waitForEvent('download');
  await page.locator('#detalle-pdf').click();
  await (await download).saveAs(path.join(output, 'presupuesto-historico.pdf'));
  const pdf = fs.readFileSync(path.join(output, 'presupuesto-historico.pdf'));
  assert.equal(pdf.subarray(0, 4).toString(), '%PDF');
  const extracted = spawnSync('pdftotext', ['-layout', path.join(output, 'presupuesto-historico.pdf'), '-'], { encoding: 'utf8' });
  if (extracted.status === 0) {
    assert.match(extracted.stdout, /17\.777,77/);
    assert.match(extracted.stdout, /12\/11\/2026/);
    assert.match(extracted.stdout, /TOTAL GUARDADO/);
    assert.match(extracted.stdout, /Importe guardado en el presupuesto/);
    assert.doesNotMatch(extracted.stdout, /Precio del Sin fecha|Invalid Date|NaN/);
  }
  await page.setViewportSize({ width: 390, height: 844 });
  assert.ok(await page.locator('#notice').evaluate(el => {
    const close = el.querySelector('button').getBoundingClientRect();
    return document.elementFromPoint(close.x + close.width/2, close.y + close.height/2)?.closest('#notice') === el;
  }));
  await page.screenshot({ path: path.join(output, 'historial-detalle-mobile.png') });
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
  await page.locator('#detalle-dialogo [data-close]').click();
  await page.screenshot({ path: path.join(output, 'historial-mobile.png'), fullPage: true });
  failDetail = true;
  await page.locator('[data-pdf="41"]').first().click();
  await page.waitForFunction(() => !document.getElementById('notice').hidden && document.getElementById('notice').textContent.includes('no pertenece'));
  assert.equal(downloads.length, 1);
  failDetail = false;
  mode = 'page'; await page.locator('#actualizar').click();
  await page.getByText('Página 1 de 2', { exact: true }).waitFor();
  await page.locator('#siguiente').click();
  await page.getByText('Página 2 de 2', { exact: true }).waitFor();
  assert.equal(await page.locator('.saved-budget').count(), 1);
  mode = 'error'; await page.locator('#actualizar').click();
  await page.getByRole('button', { name: 'Reintentar', exact: true }).waitFor();
  mode = 'empty'; await page.getByRole('button', { name: 'Reintentar', exact: true }).click();
  await page.getByText('La próxima juntada empieza acá', { exact: true }).waitFor();
  mode = 'array'; await page.locator('#actualizar').click();
  await page.locator('.saved-budget').nth(2).waitFor();
  delayed = true;
  await page.locator('[data-pdf="41"]').first().click();
  await page.locator('#logout').click();
  await page.getByRole('button', { name: 'Ingresar a mi cuenta' }).waitFor();
  await page.waitForTimeout(250);
  assert.equal(downloads.length, 1);
  await page.getByRole('button', { name: 'Ingresar a mi cuenta' }).click();
  await page.locator('#auth-form [name=email]').fill('prueba@example.com');
  await page.locator('#auth-password').fill('ClaveDePrueba123');
  await page.locator('#auth-submit').click();
  await page.locator('.saved-budget').nth(2).waitFor();
  await page.setViewportSize({ width: 1024, height: 900 });
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
  const classicPdf = await page.evaluate(async () => {
    const { generarPDF } = await import('/assets/js/reporte.js');
    const blob = await generarPDF({ localNombre: 'Local actual', direccion: 'Calle 123', generado: '2026-11-13', total: 25000, items: [{ nombre: 'Asado', cantidad: 1, precio: 25000, subtotal: 25000, fecha: '2026-11-13' }] });
    return blob.type;
  });
  assert.equal(classicPdf, 'application/pdf');
  assert.ok(calls.filter(c => c.path.startsWith('/api/presupuestos/')).length >= 4);
  assert.equal(calls.filter(c => c.path.startsWith('/api/presupuestos') && c.method !== 'GET').length, 0);
  assert.equal(calls.filter(c => ['/api/precios', '/api/productos', '/api/locales'].includes(c.path)).length, 0);
  assert.deepEqual(errors, []);
  console.log('OK: historial, filtros/orden, detalle móvil, PDF histórico, ausencia de fecha de precio, Page/array, vacío/error, login/logout, cancelación por cambio de sesión, PDF público y protección de texto.');
  console.log('Archivos de prueba:', output);
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  if (browser) await browser.close();
  server.close();
});
