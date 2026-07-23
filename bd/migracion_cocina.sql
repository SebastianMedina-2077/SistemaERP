-- =====================================================================
--  ERP "Mamma Tomato" (pizzeria-erp) - Migracion incremental: cocina (KDS)
--
--  Para bases YA creadas con una estructura anterior al tablero de cocina
--  con checklist por item. (En instalaciones nuevas no hace falta: la
--  estructura consolidada bd/MamaTomato_V0.21.sql ya incluye esto.)
--
--  Anade:
--   - `detalle_pedido.servido`: marca de "item servido" (checklist de cocina).
--     Cuando TODOS los items de un pedido quedan servidos, el backend pasa el
--     pedido a ATENDIDO de forma automatica. Por defecto 0 (no servido).
--
--  Idempotente: se consulta INFORMATION_SCHEMA antes del ALTER (MySQL 8 no
--  admite IF NOT EXISTS en ADD COLUMN) con sentencia preparada, para poder
--  re-ejecutar la migracion sin error.
-- =====================================================================

USE `erp_mamatomato`;

-- Marca de item servido en la linea del pedido (checklist tipo lista de compras del KDS).
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `detalle_pedido` ADD COLUMN `servido` tinyint(1) NOT NULL DEFAULT 0 AFTER `observacion`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'detalle_pedido' AND COLUMN_NAME = 'servido');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
--  Fin de la migracion de cocina (KDS)
-- =====================================================================
