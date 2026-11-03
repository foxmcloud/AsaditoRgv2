# Asadito RG

Comparador de precios de cortes de asado de locales de Río Grande. Permite armar una lista en kilos, comparar el total por comercio, generar un presupuesto PDF y compartir su resumen por WhatsApp.

## Desarrollo local

Requisitos: JDK completo 17 recomendado (código compatible con Java 11), PostgreSQL y acceso a Maven Central/Gradle para instalar dependencias. No se necesita Node para servir el frontend: HTML, CSS y JavaScript se sirven desde Spring Boot, sin CDN.

1. Crear una base PostgreSQL `appasadito` con un usuario propio.
2. Configurar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` en el entorno del proceso. No guardar contraseñas en Git.
3. Si usás una base de la versión anterior, respaldarla, detener la aplicación y ejecutar `docs/migracion-v2.sql` antes del primer arranque. No ejecutar sobre producción sin revisar la migración.
4. Para crear el primer administrador, configurar `ASADITO_ADMIN_EMAIL` y `ASADITO_ADMIN_PASSWORD` (10 caracteres mínimo, 72 bytes UTF-8 máximo). No hay una contraseña predeterminada. Una vez creado el administrador, retirar esas dos variables y reiniciar. El registro público solo crea usuarios con rol USER.
5. Ejecutar `bash gradlew bootRun` y abrir el puerto configurado (`8080` por defecto) desde tu entorno local.
6. Para crear el primer operador, configurar `ASADITO_OPERADOR_EMAIL` y `ASADITO_OPERADOR_PASSWORD`, con un email distinto al del administrador y una contraseña de al menos 10 caracteres (máximo 72 bytes UTF-8). Después de crearlo, retirar las variables. Ingresar como operador y cargar precios en la página independiente `/gestion.html` (“Panel de operador”). Se crean 8 cortes y 13 locales iniciales; no se inventan precios ni se importan los Excel automáticamente.

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
```

Las pruebas usan H2 aislada en modo PostgreSQL y no modifican la base de desarrollo. Verifican seguridad, sesión, hash, ABM, validaciones, bajas lógicas, cálculo decimal y PDF. La aplicación también se probó en PostgreSQL y en Chromium con una base efímera independiente.

## Guía para estudiar y continuar

Leé [la guía de desarrollo y preparación del examen](docs/GUIA_DESARROLLO.md). Incluye correspondencia con los criterios del PDF de la materia, explicación del código, pruebas manuales, pendientes y una ruta para implementar WhatsApp Business más adelante. También está disponible en [PDF](docs/GUIA_DESARROLLO.pdf).

Comparar y calcular es público. Descargar el PDF requiere sesión. Agregar y modificar el catálogo requiere OPERADOR. Dar de baja requiere ADMIN; el administrador no puede crear ni editar. WhatsApp recibe el resumen de texto mediante `wa.me`; el PDF se adjunta manualmente o se comparte desde el menú del dispositivo si soporta Web Share de archivos. La aplicación no manda mensajes automáticamente.

## Paneles por rol

La página pública `/` contiene solo comparador, calculadora y reportes. La gestión está separada en `/gestion.html`, con ingreso y acciones según el rol. Sus módulos JavaScript también están separados: `app.js` (público), `gestion.js` (panel) y `session.js` (autenticación compartida).

| Rol | Panel | Acciones sobre cortes, locales y precios |
| --- | --- | --- |
| USER | Sin gestión | Comparar, calcular y descargar PDF con sesión |
| OPERADOR | Panel de operador | Agregar y modificar; no puede eliminar |
| ADMIN | Administrar bajas | Dar de baja lógicamente; no puede agregar ni modificar |

Los roles se validan en Spring Security por método y ruta, además de ocultar los botones que no corresponden. Las cuentas ADMIN existentes conservan su rol y ahora tienen exclusivamente permisos de baja sobre el catálogo. No hay que cambiar el esquema de la base.

Para IntelliJ, revisar [la configuración de cuentas y paneles](docs/ROLES_Y_PANELES.md).

Podés revisar [un ejemplo del nuevo presupuesto PDF](docs/EJEMPLO_PRESUPUESTO.pdf), con datos explícitamente ficticios y sin cargarlos en el catálogo.
