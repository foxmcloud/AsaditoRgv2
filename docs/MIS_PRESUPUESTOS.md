# Mis presupuestos: integración del frontend

La página `/mis-presupuestos.html` reutiliza el logo, `app.css`, el inicio de sesión de `session.js` y el generador `reporte.js`. Conserva búsqueda y ordenamiento del HTML compartido y muestra tarjetas también en el detalle móvil. El enlace aparece en la navegación al iniciar sesión.

## Contrato que utiliza

- `GET /api/presupuestos?page=0&size=12`: array de cabeceras o `Page` de Spring con `content`, `number` y `totalPages`. Búsqueda y orden filtran la página recibida; no se ocultan las páginas restantes.
- `GET /api/presupuestos/{id}`: la respuesta del controlador compartido: `id`, `fecha`, `localNombre`, `localDireccion`, `moneda`, `total` y `detalles` con `corteNombre`, `cantidad`, `precioKg` y `subtotal`.
- Sesión, login, logout y CSRF: las rutas actuales del proyecto.

Si tu controlador no tiene el prefijo `/api`, cambiá solamente `PRESUPUESTOS_URL` al comienzo de `assets/js/mis-presupuestos.js`.

Cada descarga vuelve a consultar el detalle. El control `findByIdAndUsuario_Id` del backend debe conservarse. Los importes del PDF vienen del registro guardado: el frontend no consulta el catálogo ni vuelve a multiplicar precios. No hace POST de presupuestos al regenerar el archivo. Si cambia la cuenta o se cierra sesión durante la descarga, se cancela su entrega.

`presupuesto-guardado.js` transforma la respuesta al formato de `reporte.js`. No se usa el ID del detalle como si fuera el ID del producto. Como el controlador no devuelve `fechaPrecio`, el PDF muestra “Importe guardado en el presupuesto”; no inventa una fecha. Diferencia la fecha de guardado de la fecha de regeneración. Las fechas con hora/zona se muestran en Argentina; un `LocalDate` conserva su día.

Se admiten importes ARS, como en la calculadora actual. Valores o detalles ausentes se informan como error, sin reemplazarlos por cero. Los errores 404 de un detalle no generan archivos. Los textos se escapan al mostrar nombres y direcciones.

## Backend

El backend actual incluye PresupuestoController, incorporado desde tu ZIP, con listado propio, detalle por ID y guardado. Descargar desde la calculadora guarda un presupuesto; regenerar un PDF desde el historial solo lo consulta y no crea otro registro. El DTO del listado usa localnombre y el del detalle localNombre; el frontend admite ambos nombres.

La configuración actual de Spring permite acceder a la página HTML únicamente con sesión, mediante `anyRequest().authenticated()`. Por eso se accede desde el enlace que aparece tras ingresar en el comparador. La página también contempla una sesión que vence mientras está abierta. Si decidís que el HTML pueda abrirse sin cuenta para mostrar el formulario, podés permitir únicamente `/mis-presupuestos.html` en las páginas públicas; los endpoints de datos deben seguir autenticados y comprobar el dueño. No hace falta hacer público el historial.

## Verificación

```bash
node --test src/test/frontend/*.test.mjs
# Opcional, con Playwright y un Chromium instalado:
node src/test/frontend/mis-presupuestos.browser.cjs
```

La prueba de navegador sirve los archivos estáticos y simula el contrato JSON del controlador; no sustituye probar tu implementación Java. Comprueba tarjetas, filtros, orden, detalle móvil, paginación, PDF histórico, estados vacíos/errores, login/logout y cancelación de una descarga al cerrar sesión. También comprueba que el PDF de la calculadora siga funcionando.

`ASADITO_PREVIEW_DIR` permite elegir la carpeta de capturas/PDF de prueba. `CHROMIUM_PATH` permite indicar el ejecutable del navegador. Las pruebas usan datos ficticios y no escriben en una base real.

## Verificaciones

Cada tarjeta permite verificar la compra y el detalle muestra estimado, pagado, diferencia, porcentaje, valoración y observaciones. El botón Mis resultados abre una página con los resultados propios, filtros y compras pendientes. El enlace desde un resultado usa `/mis-presupuestos.html?presupuesto=ID` para abrir directamente su detalle.

Consultá [la guía de verificaciones](VERIFICACIONES_IMPLEMENTACION.md) para el contrato, las validaciones y los pequeños ajustes de Java necesarios.
