# Asadito RG

Comparador de precios de cortes de asado de locales de Río Grande. Permite armar una lista en kilos, comparar el total por comercio, generar un presupuesto PDF y compartir su resumen por WhatsApp.

## Desarrollo local

Requisitos: JDK completo 17 recomendado (código compatible con Java 11), PostgreSQL y acceso a Maven Central/Gradle para instalar dependencias. No se necesita Node para servir el frontend: HTML, CSS y JavaScript se sirven desde Spring Boot, sin CDN.

1. Crear una base PostgreSQL `appasadito` con un usuario propio.
2. Configurar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` en el entorno del proceso. No guardar contraseñas en Git.
3. Si tu base todavía no tiene las columnas `activo`, revisar `docs/migracion-v2.sql` con respaldo y la aplicación detenida. Una base que ya funcionaba con los paneles anteriores no necesita repetir la migración. La simplificación de Java conserva las tablas y cuentas existentes.
4. Para crear el primer administrador, configurar `ASADITO_ADMIN_EMAIL` y `ASADITO_ADMIN_PASSWORD` (10 caracteres mínimo, 72 bytes UTF-8 máximo). No hay una contraseña predeterminada. Una vez creado el administrador, retirar esas dos variables y reiniciar. El registro público solo crea usuarios con rol USER.
5. Ejecutar `bash gradlew bootRun` y abrir el puerto configurado (`8080` por defecto) desde tu entorno local.
6. Ingresar como ADMIN en `/gestion.html#operadores` y usar **Crear operador**. Definir un email nuevo y una contraseña propia (mínimo 10 caracteres, máximo 72 bytes UTF-8). Esa cuenta podrá ingresar al Panel de operador y cargar precios. Como alternativa inicial se conservan `ASADITO_OPERADOR_EMAIL`/`ASADITO_OPERADOR_PASSWORD`; después de crear la cuenta, retirar esas variables. Se crean 8 cortes y 13 locales iniciales; no se inventan precios ni se importan los Excel automáticamente.

En el entorno cloud ya preparado:

```bash
bash /workspace/.asadito-env/postgres.sh
bash /workspace/.asadito-env/gradle.sh build
bash /workspace/.asadito-env/gradle.sh bootRun
```

Los helpers redirigen build y cachés a `/workspace/.asadito-env` porque este repositorio ya tenía archivos `build/` versionados. El helper de Gradle configura la base local de desarrollo; para otra base, usar propiedades de Spring en los argumentos de bootRun o ajustar el helper local. La base local permite acceso sin contraseña únicamente dentro del entorno aislado y se publica en loopback; para despliegue usar autenticación real y HTTPS.

## Pruebas

```bash
bash gradlew test
bash gradlew build
# Opcional: pruebas de la calculadora, con Node.js 22 o superior
node --test src/test/frontend/presupuesto.test.mjs
```

Las pruebas usan H2 aislada en modo PostgreSQL y no modifican la base de desarrollo. Las 11 pruebas de Java verifican arranque, DTO, seguridad, sesión, hash, ABM, validaciones, bajas lógicas y creación de cuentas. La calculadora y el PDF ahora están en JavaScript; no se generan en Java. La aplicación también se probó en PostgreSQL y en Chromium con una base efímera independiente.

## Guía para estudiar y continuar

La guía principal es [Java original y sesiones](docs/CAMBIOS_JAVA_Y_SESIONES.md), también en [PDF](docs/CAMBIOS_JAVA_Y_SESIONES.pdf). Enumera todos los archivos Java conservados o agregados, compara el inicio de sesión con ModificaClient.pdf y propone ejercicios para explicarlo. La [guía de desarrollo](docs/GUIA_DESARROLLO.md) mantiene una referencia breve a los criterios de la materia.

Para programar las siguientes funciones a mano, consultá la [guía de ampliaciones del backend en PDF](docs/GUIA_AMPLIACIONES_BACKEND.pdf) o su [versión editable](docs/GUIA_AMPLIACIONES_BACKEND.md). Propone movimientos, presupuestos guardados, verificaciones, confirmación de email y un chatbot opcional de WhatsApp; esas funciones todavía no están implementadas.

Comparar y calcular es público. El botón de descarga solicita sesión; el PDF se calcula y genera en el navegador como presupuesto orientativo, sin endpoints Java de presupuestos. ADMIN agrega, modifica y da de baja cortes y locales. OPERADOR solo publica precios nuevos; no edita ni elimina registros. Los precios publicados se conservan como historial. WhatsApp recibe el resumen de texto mediante `wa.me`; el PDF se adjunta manualmente o se comparte desde el menú del dispositivo si soporta Web Share de archivos. La aplicación no manda mensajes automáticamente.

## Paneles por rol

La página pública `/` contiene solo comparador, calculadora y reportes. La gestión está separada en `/gestion.html`, con ingreso y acciones según el rol. Sus módulos JavaScript también están separados: `app.js` (público), `gestion.js` (panel) y `session.js` (autenticación compartida).

| Rol | Panel | Acciones sobre cortes, locales y precios |
| --- | --- | --- |
| USER | Sin gestión | Comparar, calcular y descargar PDF con sesión |
| OPERADOR | Panel de operador | Solo cargar precios nuevos |
| ADMIN | Administrar catálogo | Gestionar cortes/locales, crear operadores y consultar precios |

Los roles se validan en Spring Security por método y ruta, además de ocultar los botones que no corresponden. Las cuentas existentes conservan su rol; ADMIN gestiona cortes/locales y OPERADOR publica precios. No hay que cambiar el esquema de la base.

Para IntelliJ, revisar [la configuración de cuentas y paneles](docs/ROLES_Y_PANELES.md).

Podés revisar [un ejemplo del nuevo presupuesto PDF](docs/EJEMPLO_PRESUPUESTO.pdf), con datos ficticios de una base temporal de prueba, sin cargarlos en el catálogo de desarrollo.

## Comparador en celulares

En pantallas de hasta 760 px, el comparador utiliza un selector de corte y tarjetas de comercios ordenadas numéricamente de menor a mayor precio por kilo. Muestra dirección, fecha y el menor precio publicado (incluidos empates). Los comercios sin un precio válido para ese corte no participan. Un precio publicado no garantiza stock: debe confirmarse con el local. Actualizar conserva el corte seleccionado y reordena las tarjetas con los nuevos valores; los cortes sin precios muestran un estado vacío. En escritorio se mantiene la tabla comparativa.

## Backend simplificado

Se restauraron los cuatro DTO originales (solo se corrigió una asignación faltante en PreciosDTO), los repositorios de locales y productos y PreciosService. Los controladores originales reciben los formularios con `@RequestParam`. Las sesiones se configuran en `configurations/WebAuthentication` y `WebAuthorization`, con `POST /api/login` y `POST /api/logout`, siguiendo el material del profesor y conservando CSRF. Las contraseñas y roles existentes se conservan.

La calculadora y el PDF pasaron a `presupuesto.js` y `reporte.js`; se retiraron los servicios Java y DTO adicionales de presupuestos. No hay PDFBox, JWT ni autenticación propia. Se conserva el tipo `float` original para precios, con su límite de precisión explicado en la guía.
