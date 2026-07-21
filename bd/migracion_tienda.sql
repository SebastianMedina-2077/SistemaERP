-- =====================================================================
--  ERP "Mamma Tomato" (pizzeria-erp) - Migracion incremental: tienda web
--
--  Para bases YA creadas con una estructura anterior a la tienda en linea.
--  (En instalaciones nuevas no hace falta: bd/MamaTomato_V0.21.sql y
--  bd/data.sql ya incluyen todo esto.)
--
--  Anade:
--   - `cliente_cuenta`: cuenta de acceso web de un cliente (email + JWT).
--   - `cliente_tarjeta`: tarjetas guardadas (solo datos no sensibles).
--   - `pedido_web`: detalle web del pedido (cuenta, modalidad, direccion).
--   - Usuario sistema `tienda` (rol Cajero, deshabilitado) que firma los
--     pedidos web como `id_usuario`.
-- =====================================================================

USE `erp_mamatomato`;

-- Cuenta de acceso de un cliente a la tienda en linea (login con email + JWT).
CREATE TABLE IF NOT EXISTS `cliente_cuenta` (
  `id` int NOT NULL AUTO_INCREMENT,
  `id_cliente` int NOT NULL,
  `email` varchar(120) NOT NULL,
  `password_hash` varchar(60) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  `fecha_registro` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clientecuenta_cliente` (`id_cliente`),
  UNIQUE KEY `uk_clientecuenta_email` (`email`),
  CONSTRAINT `fk_clientecuenta_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `cliente` (`id_cliente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Tarjetas guardadas del cliente web: solo marca + ultimos 4 + titular; JAMAS el numero completo ni el CVV.
CREATE TABLE IF NOT EXISTS `cliente_tarjeta` (
  `id` int NOT NULL AUTO_INCREMENT,
  `id_cuenta` int NOT NULL,
  `marca` varchar(20) NOT NULL,
  `ultimos4` char(4) NOT NULL,
  `titular` varchar(80) NOT NULL,
  `fecha_registro` datetime NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_clientetarjeta_cuenta` FOREIGN KEY (`id_cuenta`) REFERENCES `cliente_cuenta` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Detalle web de un pedido de la tienda en linea: cuenta que lo genero y
-- modalidad de entrega. La venta en si vive en `pedido` (mismo flujo del POS).
CREATE TABLE IF NOT EXISTS `pedido_web` (
  `id` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int NOT NULL,
  `id_cuenta` int NOT NULL,
  `modalidad` enum('DELIVERY','RECOJO') NOT NULL,
  `direccion` varchar(150) DEFAULT NULL,
  `fecha` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pedidoweb_pedido` (`id_pedido`),
  CONSTRAINT `fk_pedidoweb_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedido` (`id_pedido`),
  CONSTRAINT `fk_pedidoweb_cuenta` FOREIGN KEY (`id_cuenta`) REFERENCES `cliente_cuenta` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Usuario sistema de la tienda web: firma los pedidos online. Deshabilitado
-- (estado 0) y con hash de una clave aleatoria descartada: no permite login.
INSERT INTO `usuario` (`username`, `password`, `estado`, `es_admin_supremo`, `id_rol`, `id_empleado`)
SELECT 'tienda', '$2a$10$Yhd8dS/fyMgOXmM3UljBmuX7ZEzmiIhSxhGOm.05UJad3lK1pXbqu', 0, 0, r.`id_rol`, NULL
FROM `rol` r
WHERE r.`nombre` = 'Cajero'
  AND NOT EXISTS (SELECT 1 FROM `usuario` u WHERE u.`username` = 'tienda');

-- Imagen de producto para el catalogo de la tienda (se carga despues, ej. via Google Drive).
-- MySQL 8 no admite IF NOT EXISTS en ADD COLUMN/ADD UNIQUE KEY, asi que se consulta
-- INFORMATION_SCHEMA con sentencia preparada para que la migracion sea re-ejecutable.
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `producto` ADD COLUMN `imagen_url` varchar(500) DEFAULT NULL',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'producto' AND COLUMN_NAME = 'imagen_url');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Codigo de cupon: una promocion con codigo NO se aplica sola; su descuento
-- lo aplica la cotizacion solo cuando recibe ese codigo.
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `promocion` ADD COLUMN `codigo` varchar(20) DEFAULT NULL',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'promocion' AND COLUMN_NAME = 'codigo');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `promocion` ADD UNIQUE KEY `uk_promocion_codigo` (`codigo`)',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'promocion' AND INDEX_NAME = 'uk_promocion_codigo');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Cupon de demo: la promo del seed (10% en gaseosa) pasa a ser solo-cupon BEBIDA10.
UPDATE `promocion` SET `codigo` = 'BEBIDA10' WHERE `id_promocion` = 1 AND `codigo` IS NULL;

-- Metodo de pago de las billeteras simuladas del checkout web (PayPal/Apple Pay/Google Pay).
INSERT INTO `metodo_pago` (`descripcion`, `activo`)
SELECT 'Billetera', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `metodo_pago` m WHERE m.`descripcion` = 'Billetera');

-- Fotos reales de la carta para los productos que emparejan (prototipo); idempotente.
UPDATE `producto` SET `imagen_url` = CONCAT('/img/tienda/productos/', `codigo`, '.webp')
WHERE `codigo` IN (
  'PZ0001','PZ0002','BB0001','CB0001','PZ0003','PZ0004','PZ0005','PZ0006',
  'PZ0007','PZ0008','PZ0009','PZ0010','PZ0011','PZ0012','PZ0013','PZ0014',
  'PZ0016','PZ0017','PZ0018','PZ0019','PZ0020','PZ0021','PZ0022','PZ0023',
  'PZ0024','PZ0025','PZ0026','PZ0028','MN0001','MN0002','MN0004','PA0001',
  'PA0002','PA0003','BB0003','BB0004','BB0005','CP0001','CP0005','CP0006',
  'CB0002','CB0003','CB0004','CB0005','CB0006','CB0007','CB0008','CB0009',
  'CB0010','CB0011','CB0012');

-- Adicionales cobrables al cliente (extra queso, salsas) y su vinculo por producto.
CREATE TABLE IF NOT EXISTS `adicional` (
  `id_adicional` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(40) NOT NULL,
  `precio` decimal(6,2) NOT NULL,
  `disponible` tinyint(1) NOT NULL DEFAULT 1,
  `grupo` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id_adicional`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `producto_adicional` (
  `id_producto` int NOT NULL,
  `id_adicional` int NOT NULL,
  PRIMARY KEY (`id_producto`,`id_adicional`),
  CONSTRAINT `fk_prodadic_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`),
  CONSTRAINT `fk_prodadic_adicional` FOREIGN KEY (`id_adicional`) REFERENCES `adicional` (`id_adicional`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `detalle_pedido_adicional` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cantidad` int NOT NULL,
  `precio_unitario` decimal(6,2) NOT NULL,
  `id_detallepedido` int NOT NULL,
  `id_adicional` int NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_detadic_detalle` FOREIGN KEY (`id_detallepedido`) REFERENCES `detalle_pedido` (`id_detallepedido`),
  CONSTRAINT `fk_detadic_adicional` FOREIGN KEY (`id_adicional`) REFERENCES `adicional` (`id_adicional`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed de prototipo (a validar con la tienda): extras y salsas vinculados a las pizzas (cat 1).
INSERT INTO `adicional` (`nombre`, `precio`, `disponible`, `grupo`)
SELECT * FROM (
  SELECT 'Extra queso' AS n, 4.00 AS p, 1 AS d, 'Extra' AS g
  UNION ALL SELECT 'Doble pepperoni', 5.00, 1, 'Extra'
  UNION ALL SELECT 'Extra champinones', 3.50, 1, 'Extra'
  UNION ALL SELECT 'Crema Alioli', 2.90, 1, 'Salsa'
  UNION ALL SELECT 'Crema Mediterranea', 2.90, 1, 'Salsa'
  UNION ALL SELECT 'Salsa Arandano', 5.90, 1, 'Salsa'
) s
WHERE NOT EXISTS (SELECT 1 FROM `adicional`);

INSERT INTO `producto_adicional` (`id_producto`, `id_adicional`)
SELECT p.`id_producto`, a.`id_adicional`
FROM `producto` p CROSS JOIN `adicional` a
WHERE p.`id_categoria` = 1
  AND NOT EXISTS (SELECT 1 FROM `producto_adicional` pa
                  WHERE pa.`id_producto` = p.`id_producto` AND pa.`id_adicional` = a.`id_adicional`);

-- Apellidos peruanos (paterno + materno) en empleado: la columna unica `apellido`
-- se divide; lo existente pasa a `apellido_paterno` y el materno queda vacio
-- para completarlo desde el admin.
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `empleado` ADD COLUMN `apellido_paterno` varchar(30) NOT NULL DEFAULT '''' AFTER `nombre`, ADD COLUMN `apellido_materno` varchar(30) NOT NULL DEFAULT '''' AFTER `apellido_paterno`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'empleado' AND COLUMN_NAME = 'apellido_paterno');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 1,
  'UPDATE `empleado` SET `apellido_paterno` = `apellido` WHERE `apellido_paterno` = ''''',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'empleado' AND COLUMN_NAME = 'apellido');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 1,
  'ALTER TABLE `empleado` DROP COLUMN `apellido`',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'empleado' AND COLUMN_NAME = 'apellido');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Datos personales del cliente web: apellidos y ubicacion (registro de la tienda).
-- `nombre` se ensancha porque 15 quedaba corto para nombres compuestos.
ALTER TABLE `cliente` MODIFY COLUMN `nombre` varchar(40) NOT NULL;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `cliente`
     ADD COLUMN `apellido_paterno` varchar(40) DEFAULT NULL AFTER `nombre`,
     ADD COLUMN `apellido_materno` varchar(40) DEFAULT NULL AFTER `apellido_paterno`,
     ADD COLUMN `departamento` varchar(60) DEFAULT NULL,
     ADD COLUMN `provincia` varchar(60) DEFAULT NULL,
     ADD COLUMN `distrito` varchar(60) DEFAULT NULL,
     ADD COLUMN `direccion_exacta` varchar(50) DEFAULT NULL,
     ADD COLUMN `referencia` varchar(50) DEFAULT NULL',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'erp_mamatomato' AND TABLE_NAME = 'cliente' AND COLUMN_NAME = 'apellido_paterno');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
--  Fin de la migracion de la tienda web
-- =====================================================================
