-- =====================================================================
--  ERP "Mamma Tomato" (pizzeria-erp) - Estructura completa de la base de datos
--  Esquema oficial del proyecto (v0.02). Contrastado por ddl-auto=validate.
--
--  Script autocontenido: crea la base, todas las tablas y las relaciones.
--  Ejecutar directo en MySQL 8. NO incluye datos (ver bd/seed.sql para los
--  datos iniciales: roles, metodos de pago, medidas, usuarios, etc.).
--
--  Novedad v0.02: tabla `pago` para pago mixto (1 fila = pago simple,
--  2+ filas = efectivo + tarjeta/yape/plin sobre un mismo pedido).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `erp_mamatomato`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `erp_mamatomato`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `auditoria`;
DROP TABLE IF EXISTS `detalle_movimiento`;
DROP TABLE IF EXISTS `movimiento`;
DROP TABLE IF EXISTS `detalle_compra`;
DROP TABLE IF EXISTS `compra`;
DROP TABLE IF EXISTS `pago`;
DROP TABLE IF EXISTS `boleta`;
DROP TABLE IF EXISTS `detalle_pedido`;
DROP TABLE IF EXISTS `pedido`;
DROP TABLE IF EXISTS `promocion_producto`;
DROP TABLE IF EXISTS `promocion`;
DROP TABLE IF EXISTS `producto_insumo`;
DROP TABLE IF EXISTS `combo_producto`;
DROP TABLE IF EXISTS `producto`;
DROP TABLE IF EXISTS `insumo`;
DROP TABLE IF EXISTS `medida`;
DROP TABLE IF EXISTS `categoria`;
DROP TABLE IF EXISTS `metodo_pago`;
DROP TABLE IF EXISTS `cliente`;
DROP TABLE IF EXISTS `usuario`;
DROP TABLE IF EXISTS `empleado`;
DROP TABLE IF EXISTS `rol`;
DROP TABLE IF EXISTS `proveedor`;
DROP TABLE IF EXISTS `tipo_movimiento`;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
--  Catalogos / tablas base (sin dependencias)
-- ---------------------------------------------------------------------

CREATE TABLE `rol` (
  `id_rol` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(15) NOT NULL,
  PRIMARY KEY (`id_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `empleado` (
  `id_empleado` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(20) NOT NULL,
  `apellido` varchar(20) NOT NULL,
  `dni` char(8) NOT NULL,
  `telefono` char(9) NOT NULL,
  `cargo` varchar(15) NOT NULL,
  PRIMARY KEY (`id_empleado`),
  UNIQUE KEY `uk_empleado_dni` (`dni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `usuario` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `username` varchar(10) NOT NULL,
  `password` varchar(60) NOT NULL,
  `estado` tinyint(1) NOT NULL,
  `es_admin_supremo` tinyint(1) NOT NULL DEFAULT 0,
  `id_rol` int NOT NULL,
  `id_empleado` int DEFAULT NULL,
  PRIMARY KEY (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cliente` (
  `id_cliente` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(15) NOT NULL,
  `telefono` char(9) DEFAULT NULL,
  PRIMARY KEY (`id_cliente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `proveedor` (
  `id_proveedor` int NOT NULL AUTO_INCREMENT,
  `ruc` char(11) NOT NULL,
  `nombre` varchar(25) NOT NULL,
  `telefono` char(9) NOT NULL,
  `direccion` varchar(25) NOT NULL,
  PRIMARY KEY (`id_proveedor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `metodo_pago` (
  `id_metodopago` int NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(15) DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_metodopago`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `categoria` (
  `id_categoria` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(15) DEFAULT NULL,
  PRIMARY KEY (`id_categoria`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medida` (
  `id_medida` int NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(15) NOT NULL,
  `sigla` varchar(4) NOT NULL,
  PRIMARY KEY (`id_medida`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tipo_movimiento` (
  `id_tipomovimiento` int NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(15) NOT NULL,
  `operacion` varchar(15) NOT NULL,
  PRIMARY KEY (`id_tipomovimiento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
--  Catalogo de productos e insumos
-- ---------------------------------------------------------------------

CREATE TABLE `insumo` (
  `id_insumo` int NOT NULL AUTO_INCREMENT,
  `codigo` char(6) NOT NULL,
  `nombre` varchar(25) NOT NULL,
  `precio` decimal(6,2) NOT NULL,
  `estado` enum('normal','bajo') NOT NULL,
  `stock` decimal(8,3) NOT NULL,
  `cantidad_minima` decimal(8,3) NOT NULL,
  `id_medida` int NOT NULL,
  PRIMARY KEY (`id_insumo`),
  UNIQUE KEY `uk_insumo_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `producto` (
  `id_producto` int NOT NULL AUTO_INCREMENT,
  `codigo` char(6) NOT NULL,
  `nombre` varchar(60) NOT NULL,
  `precio` decimal(6,2) NOT NULL,
  `stock` int DEFAULT NULL,
  `tamanio` enum('personal','mediano','familiar') DEFAULT NULL,
  `disponible` tinyint(1) NOT NULL,
  `id_categoria` int NOT NULL,
  PRIMARY KEY (`id_producto`),
  UNIQUE KEY `uk_producto_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Receta: insumos que componen un producto.
CREATE TABLE `producto_insumo` (
  `cantidad` decimal(6,3) NOT NULL,
  `id_insumo` int NOT NULL,
  `id_producto` int NOT NULL,
  PRIMARY KEY (`id_insumo`,`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Combos: productos que agrupan otros productos.
CREATE TABLE `combo_producto` (
  `cantidad` int NOT NULL,
  `id_producto` int NOT NULL,
  `id_combo` int NOT NULL,
  PRIMARY KEY (`id_producto`,`id_combo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promocion` (
  `id_promocion` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(25) DEFAULT NULL,
  `descripcion` varchar(25) DEFAULT NULL,
  `tipo_descuento` varchar(20) DEFAULT NULL,
  `valor_descuento` decimal(8,2) DEFAULT NULL,
  `activa` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id_promocion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promocion_producto` (
  `id_promocion` int NOT NULL,
  `id_producto` int NOT NULL,
  `cantidad_minima` int NOT NULL,
  PRIMARY KEY (`id_promocion`,`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
--  Ventas: pedido -> detalle, boleta y pagos
-- ---------------------------------------------------------------------

CREATE TABLE `pedido` (
  `id_pedido` int NOT NULL AUTO_INCREMENT,
  `fecha` datetime NOT NULL,
  `estado` enum('PENDIENTE','PREPARANDO','ATENDIDO','ANULADO') NOT NULL,
  `motivo_anulacion` varchar(100) DEFAULT NULL,
  `id_usuario` int NOT NULL,
  `id_cliente` int NOT NULL,
  PRIMARY KEY (`id_pedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `detalle_pedido` (
  `id_detallepedido` int NOT NULL AUTO_INCREMENT,
  `cantidad` int NOT NULL,
  `precio_unitario` decimal(8,2) DEFAULT NULL,
  `subtotal` decimal(8,2) NOT NULL DEFAULT '0.00',
  `descuento` decimal(8,2) DEFAULT NULL,
  `observacion` varchar(100) DEFAULT NULL,
  `id_pedido` int NOT NULL,
  `id_producto` int NOT NULL,
  PRIMARY KEY (`id_detallepedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `boleta` (
  `id_boleta` int NOT NULL AUTO_INCREMENT,
  `subtotal` decimal(6,2) NOT NULL,
  `igv` decimal(8,2) DEFAULT NULL,
  `total` decimal(8,2) NOT NULL,
  `id_metodopago` int NOT NULL,
  `id_pedido` int NOT NULL,
  PRIMARY KEY (`id_boleta`),
  UNIQUE KEY `uk_boleta_pedido` (`id_pedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Desglose de pago de un pedido: 1 fila = pago simple, 2+ filas = pago mixto
-- (ej. parte en efectivo y parte con tarjeta/yape).
CREATE TABLE `pago` (
  `id_pago` int NOT NULL AUTO_INCREMENT,
  `monto` decimal(8,2) NOT NULL,
  `id_pedido` int NOT NULL,
  `id_metodopago` int NOT NULL,
  PRIMARY KEY (`id_pago`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
--  Compras e inventario (kardex)
-- ---------------------------------------------------------------------

CREATE TABLE `compra` (
  `id_compra` int NOT NULL AUTO_INCREMENT,
  `fecha` date NOT NULL,
  `total` decimal(8,2) NOT NULL,
  `estado` varchar(20) DEFAULT NULL,
  `id_proveedor` int NOT NULL,
  `id_usuario` int NOT NULL,
  PRIMARY KEY (`id_compra`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `detalle_compra` (
  `id_detallecompra` int NOT NULL AUTO_INCREMENT,
  `cantidad` decimal(8,3) NOT NULL,
  `precio_unitario` decimal(6,2) NOT NULL,
  `subtotal` decimal(6,2) NOT NULL,
  `id_compra` int NOT NULL,
  `id_insumo` int NOT NULL,
  PRIMARY KEY (`id_detallecompra`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `movimiento` (
  `id_movimiento` int NOT NULL AUTO_INCREMENT,
  `documento` varchar(15) DEFAULT NULL,
  `fecha` date NOT NULL,
  `glosa` varchar(25) DEFAULT NULL,
  `id_tipomovimiento` int NOT NULL,
  `id_compra` int DEFAULT NULL,
  `id_usuario` int NOT NULL,
  PRIMARY KEY (`id_movimiento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `detalle_movimiento` (
  `id_detallemovimiento` int NOT NULL AUTO_INCREMENT,
  `cantidad` decimal(8,3) NOT NULL,
  `stock_resultante` decimal(8,3) DEFAULT NULL,
  `id_insumo` int NOT NULL,
  `id_movimiento` int NOT NULL,
  PRIMARY KEY (`id_detallemovimiento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
--  Auditoria: log de acciones de escritura del personal.
--  Sin FK a usuario: guarda el username como texto para conservar el rastro
--  aunque el usuario llegara a eliminarse.
-- ---------------------------------------------------------------------

CREATE TABLE `auditoria` (
  `id_auditoria` int NOT NULL AUTO_INCREMENT,
  `fecha` datetime NOT NULL,
  `usuario` varchar(50) NOT NULL,
  `accion` varchar(20) NOT NULL,
  `entidad` varchar(40) NOT NULL,
  `referencia` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`id_auditoria`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================
--  Relaciones (FOREIGN KEYs). Todas RESTRICT: los borrados con
--  dependencias se controlan en la capa de servicio.
-- =====================================================================

ALTER TABLE `usuario`
  ADD CONSTRAINT `fk_usuario_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id_rol`),
  ADD CONSTRAINT `fk_usuario_empleado` FOREIGN KEY (`id_empleado`) REFERENCES `empleado` (`id_empleado`);

ALTER TABLE `insumo`
  ADD CONSTRAINT `fk_insumo_medida` FOREIGN KEY (`id_medida`) REFERENCES `medida` (`id_medida`);

ALTER TABLE `producto`
  ADD CONSTRAINT `fk_producto_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categoria` (`id_categoria`);

ALTER TABLE `producto_insumo`
  ADD CONSTRAINT `fk_productoinsumo_insumo` FOREIGN KEY (`id_insumo`) REFERENCES `insumo` (`id_insumo`),
  ADD CONSTRAINT `fk_productoinsumo_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`);

ALTER TABLE `combo_producto`
  ADD CONSTRAINT `fk_comboproducto_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`),
  ADD CONSTRAINT `fk_comboproducto_combo` FOREIGN KEY (`id_combo`) REFERENCES `producto` (`id_producto`);

ALTER TABLE `promocion_producto`
  ADD CONSTRAINT `fk_promocionproducto_promocion` FOREIGN KEY (`id_promocion`) REFERENCES `promocion` (`id_promocion`),
  ADD CONSTRAINT `fk_promocionproducto_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`);

ALTER TABLE `pedido`
  ADD CONSTRAINT `fk_pedido_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`),
  ADD CONSTRAINT `fk_pedido_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `cliente` (`id_cliente`);

ALTER TABLE `detalle_pedido`
  ADD CONSTRAINT `fk_detallepedido_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`),
  ADD CONSTRAINT `fk_detallepedido_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`);

ALTER TABLE `boleta`
  ADD CONSTRAINT `fk_boleta_metodopago` FOREIGN KEY (`id_metodopago`) REFERENCES `metodo_pago` (`id_metodopago`),
  ADD CONSTRAINT `fk_boleta_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`);

ALTER TABLE `pago`
  ADD CONSTRAINT `fk_pago_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`),
  ADD CONSTRAINT `fk_pago_metodopago` FOREIGN KEY (`id_metodopago`) REFERENCES `metodo_pago` (`id_metodopago`);

ALTER TABLE `compra`
  ADD CONSTRAINT `fk_compra_proveedor` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedor` (`id_proveedor`),
  ADD CONSTRAINT `fk_compra_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`);

ALTER TABLE `detalle_compra`
  ADD CONSTRAINT `fk_detallecompra_compra` FOREIGN KEY (`id_compra`) REFERENCES `compra` (`id_compra`),
  ADD CONSTRAINT `fk_detallecompra_insumo` FOREIGN KEY (`id_insumo`) REFERENCES `insumo` (`id_insumo`);

ALTER TABLE `movimiento`
  ADD CONSTRAINT `fk_movimiento_tipomovimiento` FOREIGN KEY (`id_tipomovimiento`) REFERENCES `tipo_movimiento` (`id_tipomovimiento`),
  ADD CONSTRAINT `fk_movimiento_compra` FOREIGN KEY (`id_compra`) REFERENCES `compra` (`id_compra`),
  ADD CONSTRAINT `fk_movimiento_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`);

ALTER TABLE `detalle_movimiento`
  ADD CONSTRAINT `fk_detallemovimiento_insumo` FOREIGN KEY (`id_insumo`) REFERENCES `insumo` (`id_insumo`),
  ADD CONSTRAINT `fk_detallemovimiento_movimiento` FOREIGN KEY (`id_movimiento`) REFERENCES `movimiento` (`id_movimiento`);

-- =====================================================================
--  Fin del script de estructura (v0.02)
-- =====================================================================
