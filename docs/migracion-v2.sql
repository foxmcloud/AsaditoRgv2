-- Ejecutar con copia de seguridad y aplicación detenida, antes del primer arranque v2.
-- Idempotente. Las bases nuevas no necesitan el script.
BEGIN;
ALTER TABLE IF EXISTS locales ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE IF EXISTS productos ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE IF EXISTS precios ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE IF EXISTS precios ALTER COLUMN precio TYPE numeric(14,2) USING round(precio::numeric, 2);
COMMIT;
