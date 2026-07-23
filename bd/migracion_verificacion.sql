-- =====================================================================
--  ERP "Mamma Tomato" (pizzeria-erp) - Migracion incremental: verificacion de correo
--
--  Para bases YA creadas antes de la verificacion de correo de la tienda web.
--  (En instalaciones nuevas no hace falta: bd/MamaTomato_V0.21.sql ya la incluye.)
--
--  Anade:
--   - `cliente_cuenta.email_verificado`: marca si el cliente confirmo su correo con
--     el codigo de 6 digitos (verificacion blanda; el codigo vive en memoria, no en BD).
--
--  Re-ejecutable: MySQL 8 no admite IF NOT EXISTS en ADD COLUMN, asi que se consulta
--  INFORMATION_SCHEMA con sentencia preparada.
-- =====================================================================

USE `erp_mamatomato`;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `cliente_cuenta` ADD COLUMN `email_verificado` tinyint(1) NOT NULL DEFAULT 0 AFTER `activo`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'cliente_cuenta' AND COLUMN_NAME = 'email_verificado');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
--  Fin de la migracion de verificacion de correo
-- =====================================================================
