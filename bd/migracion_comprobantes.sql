-- =====================================================================
--  Migracion: comprobantes (boleta/factura/boleta electronica) + correlativo
--  sin huecos + registro de RUC. Aplicar una sola vez sobre la BD existente.
--  Uso: mysql -u root -p erp_mamatomato < bd/migracion_comprobantes.sql
-- =====================================================================

ALTER TABLE `boleta`
  ADD COLUMN `tipo_comprobante` varchar(20) NOT NULL DEFAULT 'BOLETA' AFTER `total`,
  ADD COLUMN `serie` varchar(4) NOT NULL DEFAULT 'B001' AFTER `tipo_comprobante`,
  ADD COLUMN `correlativo` int NOT NULL DEFAULT 0 AFTER `serie`,
  ADD COLUMN `cliente_documento` varchar(11) DEFAULT NULL AFTER `correlativo`,
  ADD COLUMN `cliente_razon_social` varchar(120) DEFAULT NULL AFTER `cliente_documento`,
  ADD COLUMN `cliente_email` varchar(120) DEFAULT NULL AFTER `cliente_razon_social`,
  ADD COLUMN `mesa` varchar(20) DEFAULT NULL AFTER `cliente_email`,
  ADD COLUMN `email_estado` varchar(20) DEFAULT NULL AFTER `mesa`;

CREATE TABLE IF NOT EXISTS `comprobante_correlativo` (
  `serie` varchar(4) NOT NULL,
  `ultimo_numero` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`serie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `comprobante_correlativo` (`serie`, `ultimo_numero`) VALUES
  ('B001', 0), ('F001', 0);

CREATE TABLE IF NOT EXISTS `cliente_empresa` (
  `id_cliente_empresa` int NOT NULL AUTO_INCREMENT,
  `ruc` char(11) NOT NULL,
  `razon_social` varchar(120) NOT NULL,
  PRIMARY KEY (`id_cliente_empresa`),
  UNIQUE KEY `uk_cliente_empresa_ruc` (`ruc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
