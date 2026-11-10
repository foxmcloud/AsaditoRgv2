# Diagnóstico de errores al publicar precios

Una fecha vacía, mal formada o futura se rechaza con HTTP 400. Un HTTP 409 significa que la base rechazó el guardado; no prueba que el campo Fecha sea incorrecto. En el modelo actual pueden coexistir precios del mismo corte/local/fecha: el mayor ID resuelve el empate.

La versión simplificada muestra un mensaje general para HTTP 409; se retiraron los códigos DB_* y las referencias automáticas del manejador anterior para reducir el Java. Para conocer la causa concreta, revisá la excepción de Hibernate/PostgreSQL en la consola del proceso y las restricciones de tu base. El mensaje general no alcanza para concluir que hay un duplicado de fecha.

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

Estas consultas no incluyen contraseñas ni precios de clientes. Revisá el SQLState y la definición de la restricción relevante. No borres registros ni ajustes una secuencia sin confirmar qué secuencia usa Hibernate: las entidades heredadas usan generación native y pueden compartir una secuencia (por ejemplo native); no hay que asumir que se llama hibernate_sequence. Una reparación debe considerar todas las tablas que compartan la secuencia y ejecutarse con respaldo, sin escrituras concurrentes.

## Reparación para la secuencia compartida native

Si la excepción confirma una colisión de clave primaria, las entidades siguen usando el generador native de este repositorio y comprobaste que la secuencia real es public.native, se incluye reparar-secuencia-native.sql. Con respaldo y todas las instancias detenidas, ejecutalo en la misma base que usa Spring y luego reiniciá. El script bloquea las tablas, comprueba el generador esperado y adelanta el contador por encima de los IDs de productos, locales y precios; conserva un contador que ya estuviera más adelante. No borra filas ni reasigna sus IDs. No se debe ejecutar a ciegas para otros códigos de error, otros esquemas o generadores personalizados. El cambio de una secuencia no se revierte como una escritura normal de tabla, por eso las verificaciones preceden al ajuste.
