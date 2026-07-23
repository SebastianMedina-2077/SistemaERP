-- =====================================================================
--  ERP "Mamma Tomato" (pizzeria-erp) - Estructura de la base de datos
--  Estructura consolidada v0.21 (schema v0.02 + migracion de comprobantes
--  integrada).
--
--  Consolida la antigua estructura base con la migracion de comprobantes:
--  las columnas y tablas que anadia esa migracion
--  (tipo_comprobante, serie, correlativo, cliente_documento,
--  cliente_razon_social, cliente_email, mesa, email_estado en `boleta`,
--  mas las tablas `comprobante_correlativo` y `cliente_empresa`) quedan
--  ya definidas dentro de sus CREATE TABLE. No hay ALTER sueltos: es un
--  unico script de estructura coherente y al dia.
--
--  Script autocontenido: crea la base, todas las tablas y las relaciones.
--  Ejecutar directo en MySQL 8. NO incluye datos (ver bd/data.sql para los
--  datos iniciales: roles, metodos de pago, medidas, usuarios, catalogo,
--  correlativos, etc.).
--
--  Contrastado por ddl-auto=validate contra las entidades JPA.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `erp_mamatomato`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `erp_mamatomato`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `auditoria`;
DROP TABLE IF EXISTS `detalle_pedido_adicional`;
DROP TABLE IF EXISTS `producto_adicional`;
DROP TABLE IF EXISTS `adicional`;
DROP TABLE IF EXISTS `pedido_web`;
DROP TABLE IF EXISTS `cliente_tarjeta`;
DROP TABLE IF EXISTS `cliente_cuenta`;
DROP TABLE IF EXISTS `detalle_movimiento`;
DROP TABLE IF EXISTS `movimiento`;
DROP TABLE IF EXISTS `detalle_compra`;
DROP TABLE IF EXISTS `compra`;
DROP TABLE IF EXISTS `pago`;
DROP TABLE IF EXISTS `boleta`;
DROP TABLE IF EXISTS `cliente_empresa`;
DROP TABLE IF EXISTS `comprobante_correlativo`;
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
  `apellido_paterno` varchar(30) NOT NULL,
  `apellido_materno` varchar(30) NOT NULL,
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

-- Apellidos y ubicacion los registra la tienda web; los clientes del POS pueden no tenerlos.
CREATE TABLE `cliente` (
  `id_cliente` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(40) NOT NULL,
  `apellido_paterno` varchar(40) DEFAULT NULL,
  `apellido_materno` varchar(40) DEFAULT NULL,
  `telefono` char(9) DEFAULT NULL,
  `departamento` varchar(60) DEFAULT NULL,
  `provincia` varchar(60) DEFAULT NULL,
  `distrito` varchar(60) DEFAULT NULL,
  `direccion_exacta` varchar(50) DEFAULT NULL,
  `referencia` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_cliente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Cuenta de acceso de un cliente a la tienda en linea (login con email + JWT).
CREATE TABLE `cliente_cuenta` (
  `id` int NOT NULL AUTO_INCREMENT,
  `id_cliente` int NOT NULL,
  `email` varchar(120) NOT NULL,
  `password_hash` varchar(60) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  `email_verificado` tinyint(1) NOT NULL DEFAULT 0,
  `fecha_registro` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clientecuenta_cliente` (`id_cliente`),
  UNIQUE KEY `uk_clientecuenta_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Tarjetas guardadas del cliente web: solo marca + ultimos 4 + titular; JAMAS el numero completo ni el CVV.
CREATE TABLE `cliente_tarjeta` (
  `id` int NOT NULL AUTO_INCREMENT,
  `id_cuenta` int NOT NULL,
  `marca` varchar(20) NOT NULL,
  `ultimos4` char(4) NOT NULL,
  `titular` varchar(80) NOT NULL,
  `fecha_registro` datetime NOT NULL,
  PRIMARY KEY (`id`)
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

-- Mesas del salon: solo las numeradas tienen ciclo de vida en la caja
-- (LIBRE -> OCUPADA al vender -> POR_LIMPIAR cuando el cliente se retira -> LIBRE).
-- La barra y "para llevar" no se modelan aqui (no se bloquean).
CREATE TABLE `mesa` (
  `id_mesa` int NOT NULL AUTO_INCREMENT,
  `numero` int NOT NULL,
  `capacidad` int NOT NULL DEFAULT 4,
  `estado` enum('LIBRE','OCUPADA','POR_LIMPIAR') NOT NULL DEFAULT 'LIBRE',
  PRIMARY KEY (`id_mesa`),
  UNIQUE KEY `uk_mesa_numero` (`numero`)
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
  `imagen_url` varchar(500) DEFAULT NULL,
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

-- Adicionales cobrables al cliente (extra queso, salsas, etc.). Suman precio al cotizar.
CREATE TABLE `adicional` (
  `id_adicional` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(40) NOT NULL,
  `precio` decimal(6,2) NOT NULL,
  `disponible` tinyint(1) NOT NULL DEFAULT 1,
  `grupo` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id_adicional`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Adicionales que admite cada producto.
CREATE TABLE `producto_adicional` (
  `id_producto` int NOT NULL,
  `id_adicional` int NOT NULL,
  PRIMARY KEY (`id_producto`,`id_adicional`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promocion` (
  `id_promocion` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(25) DEFAULT NULL,
  `descripcion` varchar(25) DEFAULT NULL,
  `tipo_descuento` varchar(20) DEFAULT NULL,
  `valor_descuento` decimal(8,2) DEFAULT NULL,
  `activa` tinyint(1) DEFAULT NULL,
  `codigo` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id_promocion`),
  UNIQUE KEY `uk_promocion_codigo` (`codigo`)
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
  `servido` tinyint(1) NOT NULL DEFAULT '0',
  `id_pedido` int NOT NULL,
  `id_producto` int NOT NULL,
  PRIMARY KEY (`id_detallepedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Adicionales elegidos en una linea del pedido; precio_unitario congela el precio al vender.
CREATE TABLE `detalle_pedido_adicional` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cantidad` int NOT NULL,
  `precio_unitario` decimal(6,2) NOT NULL,
  `id_detallepedido` int NOT NULL,
  `id_adicional` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Comprobante de la venta. Los campos de comprobante electronico
-- (tipo_comprobante, serie, correlativo, datos del cliente, mesa y estado
-- de envio por correo) proceden de la migracion de comprobantes, ya
-- integrados aqui como columnas de la tabla.
CREATE TABLE `boleta` (
  `id_boleta` int NOT NULL AUTO_INCREMENT,
  `subtotal` decimal(6,2) NOT NULL,
  `igv` decimal(8,2) DEFAULT NULL,
  `total` decimal(8,2) NOT NULL,
  `tipo_comprobante` varchar(20) NOT NULL DEFAULT 'BOLETA',
  `serie` varchar(4) NOT NULL DEFAULT 'B001',
  `correlativo` int NOT NULL DEFAULT 0,
  `cliente_documento` varchar(11) DEFAULT NULL,
  `cliente_razon_social` varchar(120) DEFAULT NULL,
  `cliente_email` varchar(120) DEFAULT NULL,
  `mesa` varchar(20) DEFAULT NULL,
  `email_estado` varchar(20) DEFAULT NULL,
  `id_metodopago` int NOT NULL,
  `id_pedido` int NOT NULL,
  PRIMARY KEY (`id_boleta`),
  UNIQUE KEY `uk_boleta_pedido` (`id_pedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Contador de correlativos por serie de comprobante: numeracion de negocio
-- secuencial y SIN HUECOS (el AUTO_INCREMENT de id_boleta es solo clave tecnica).
-- Tabla incorporada por la migracion de comprobantes. Las filas semilla de
-- las series (B001, F001) se cargan en bd/data.sql.
CREATE TABLE `comprobante_correlativo` (
  `serie` varchar(4) NOT NULL,
  `ultimo_numero` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`serie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Clientes con RUC (empresas) para facturacion: permite autocompletar la razon social.
-- Tabla incorporada por la migracion de comprobantes.
CREATE TABLE `cliente_empresa` (
  `id_cliente_empresa` int NOT NULL AUTO_INCREMENT,
  `ruc` char(11) NOT NULL,
  `razon_social` varchar(120) NOT NULL,
  PRIMARY KEY (`id_cliente_empresa`),
  UNIQUE KEY `uk_cliente_empresa_ruc` (`ruc`)
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

-- Detalle web de un pedido hecho desde la tienda en linea: cuenta que lo
-- genero y modalidad de entrega. La venta en si vive en `pedido` (mismo
-- flujo del POS: detalle, boleta, pagos y kardex).
CREATE TABLE `pedido_web` (
  `id` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int NOT NULL,
  `id_cuenta` int NOT NULL,
  `modalidad` enum('DELIVERY','RECOJO') NOT NULL,
  `direccion` varchar(150) DEFAULT NULL,
  `fecha` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pedidoweb_pedido` (`id_pedido`)
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

ALTER TABLE `cliente_cuenta`
  ADD CONSTRAINT `fk_clientecuenta_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `cliente` (`id_cliente`);

ALTER TABLE `cliente_tarjeta`
  ADD CONSTRAINT `fk_clientetarjeta_cuenta` FOREIGN KEY (`id_cuenta`) REFERENCES `cliente_cuenta` (`id`);

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

ALTER TABLE `producto_adicional`
  ADD CONSTRAINT `fk_prodadic_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`),
  ADD CONSTRAINT `fk_prodadic_adicional` FOREIGN KEY (`id_adicional`) REFERENCES `adicional` (`id_adicional`);

ALTER TABLE `detalle_pedido_adicional`
  ADD CONSTRAINT `fk_detadic_detalle` FOREIGN KEY (`id_detallepedido`) REFERENCES `detalle_pedido` (`id_detallepedido`),
  ADD CONSTRAINT `fk_detadic_adicional` FOREIGN KEY (`id_adicional`) REFERENCES `adicional` (`id_adicional`);

ALTER TABLE `boleta`
  ADD CONSTRAINT `fk_boleta_metodopago` FOREIGN KEY (`id_metodopago`) REFERENCES `metodo_pago` (`id_metodopago`),
  ADD CONSTRAINT `fk_boleta_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`);

ALTER TABLE `pago`
  ADD CONSTRAINT `fk_pago_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`),
  ADD CONSTRAINT `fk_pago_metodopago` FOREIGN KEY (`id_metodopago`) REFERENCES `metodo_pago` (`id_metodopago`);

ALTER TABLE `pedido_web`
  ADD CONSTRAINT `fk_pedidoweb_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`),
  ADD CONSTRAINT `fk_pedidoweb_cuenta` FOREIGN KEY (`id_cuenta`) REFERENCES `cliente_cuenta` (`id`);

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
--  Fin del script de estructura consolidada (v0.21)
-- =====================================================================
