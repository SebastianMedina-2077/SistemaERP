-- =====================================================================
--  Reinicio SEGURO de los datos de ventas (pedido y tablas hijas).
--  Deja la numeracion desde cero: id_pedido = 1 y correlativos = 0.
--  Uso: mysql -u root -p erp_mamatomato < bd/reset_pedidos.sql
--  OJO: borra TODOS los pedidos/boletas/pagos. No tocar en produccion.
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `pago`;
TRUNCATE TABLE `boleta`;
TRUNCATE TABLE `detalle_pedido`;
TRUNCATE TABLE `pedido`;
-- Cada venta crea su propio cliente anonimo; se vacian para no dejar huerfanos.
TRUNCATE TABLE `cliente`;

SET FOREIGN_KEY_CHECKS = 1;

-- TRUNCATE ya reinicia el AUTO_INCREMENT a 1; lo reforzamos por claridad.
ALTER TABLE `pedido` AUTO_INCREMENT = 1;
ALTER TABLE `detalle_pedido` AUTO_INCREMENT = 1;
ALTER TABLE `boleta` AUTO_INCREMENT = 1;
ALTER TABLE `pago` AUTO_INCREMENT = 1;
ALTER TABLE `cliente` AUTO_INCREMENT = 1;

-- Reinicia los correlativos de comprobante (numeracion oficial sin huecos).
UPDATE `comprobante_correlativo` SET `ultimo_numero` = 0;
