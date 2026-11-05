-- SOLO si se confirmó DB_ID_CONFLICT y las tres entidades usan public.native.
-- Hacer respaldo y DETENER todas las instancias de la aplicación antes de ejecutar.
-- No usar si se cambió el generador de IDs. Primero revisar DIAGNOSTICO_PRECIOS.md.
-- No borra registros ni cambia sus IDs; adelanta la secuencia compartida.
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE public.productos, public.locales, public.precios IN ACCESS EXCLUSIVE MODE;
DO $$
DECLARE limite bigint;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_sequences
        WHERE schemaname = 'public' AND sequencename = 'native'
          AND increment_by = 1 AND NOT cycle
    ) THEN
        RAISE EXCEPTION 'No se encontró la secuencia native esperada. Revisar el generador antes de continuar.';
    END IF;
    SELECT GREATEST(
        1,
        COALESCE((SELECT max(id) FROM public.productos), 0),
        COALESCE((SELECT max(id) FROM public.locales), 0),
        COALESCE((SELECT max(id) FROM public.precios), 0),
        (SELECT last_value FROM public.native)
    ) INTO limite;
    PERFORM setval('public.native'::regclass, limite, true);
END $$;
COMMIT;
