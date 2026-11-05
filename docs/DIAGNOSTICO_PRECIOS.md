# Diagnóstico de errores al publicar precios

Una fecha vacía, mal formada o futura se rechaza con HTTP 400. Un HTTP 409 significa que la base rechazó el guardado; no prueba que el campo Fecha sea incorrecto. En el modelo actual pueden coexistir precios del mismo corte/local/fecha: el mayor ID resuelve el empate.

La pantalla ahora incluye un código y una referencia. En el servidor se registra la misma referencia y el SQLState, sin imprimir el SQL ni valores enviados. El diagnóstico no cambia datos ni repara secuencias automáticamente. Hibernate también puede detectar un ID repetido antes de enviar SQL (NonUniqueObjectException o EntityExistsException); en ese caso no hay SQLState de PostgreSQL, pero ahora se muestra DB_ID_CONFLICT y se registra el tipo de causa.

| Código | Qué significa | Qué revisar |
| --- | --- | --- |
| DB_ID_CONFLICT | El ID generado colisionó con una clave primaria | Secuencia de IDs y datos importados manualmente |
| DB_DUPLICATE | Conflicto de unicidad | Restricciones únicas de la base existente |
| DB_REQUIRED_VALUE | Una columna obligatoria no recibió valor | Esquema heredado, migraciones y campos requeridos |
| DB_REFERENCE_CONFLICT | Una relación no cumple una clave foránea | Existencia del corte/local y definición de la relación |
| DB_CHECK_CONSTRAINT | Falló una regla CHECK | Definición de la regla en la base |
| DB_NUMERIC_RANGE | Un número no cabe en su columna | Precisión/tipo de columna e importe |
| DB_INTEGRITY | No hay clasificación suficiente | SQLState y referencia en la consola del proceso activo |

Si no ves el log, confirmá que mirás la consola del proceso que escucha el puerto de tu aplicación. Podés diagnosticar la estructura desde pgAdmin con estas consultas de SOLO LECTURA, en la base que usa Spring (ajustá public si utilizás otro esquema):

```sql
SELECT conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE conrelid = 'public.precios'::regclass;

SELECT column_name, data_type, is_nullable, column_default, is_identity
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'precios'
ORDER BY ordinal_position;

SELECT max(id) AS id_maximo FROM public.precios;

SELECT schemaname, sequencename, increment_by, last_value
FROM pg_sequences
WHERE schemaname = 'public';
```

Estas consultas no incluyen contraseñas ni precios de clientes. Compartí el código mostrado y la definición de la restricción relevante. No borres registros ni ajustes una secuencia sin confirmar qué secuencia usa Hibernate: las entidades heredadas usan generación native y pueden compartir una secuencia (por ejemplo native); no hay que asumir que se llama hibernate_sequence. Una reparación debe considerar todas las tablas que compartan la secuencia y ejecutarse con respaldo, sin escrituras concurrentes.

## Reparación para la secuencia compartida native

Si el código es DB_ID_CONFLICT, las entidades siguen usando el generador native de este repositorio y comprobaste que la secuencia real es public.native, se incluye reparar-secuencia-native.sql. Con respaldo y todas las instancias detenidas, ejecutalo en la misma base que usa Spring y luego reiniciá. El script bloquea las tablas, comprueba el generador esperado y adelanta el contador por encima de los IDs de productos, locales y precios; conserva un contador que ya estuviera más adelante. No borra filas ni reasigna sus IDs. No se debe ejecutar a ciegas para otros códigos de error, otros esquemas o generadores personalizados. El cambio de una secuencia no se revierte como una escritura normal de tabla, por eso las verificaciones preceden al ajuste.
