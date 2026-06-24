-- =====================================================================
--  ERP "Mamma Tomato" (pizzeria-erp) - Datos iniciales (seed)
--  Version: 0.02
--
--  Requisito: ejecutar despues de la estructura (bd_mamatomtatoV0.02.sql)
--             sobre la base erp_mamatomato.
--  Credenciales: admin/admin123 - cajero/cajero123 - cocina/cocina123
-- =====================================================================

USE `erp_mamatomato`;

-- Roles
INSERT INTO `rol` (`id_rol`, `nombre`) VALUES
  (1, 'Administrador'),
  (2, 'Cajero'),
  (3, 'Cocina');

-- Empleados
INSERT INTO `empleado` (`id_empleado`, `nombre`, `apellido`, `dni`, `telefono`, `cargo`) VALUES
  (1, 'Lucia', 'Ramos', '72814391', '987654321', 'Administrador'),
  (2, 'Mario', 'Salas', '73451298', '976431258', 'Cajero'),
  (3, 'Rosa',  'Vega',  '70234511', '965214783', 'Cocina');

-- Usuarios (password con hash BCrypt)
INSERT INTO `usuario` (`id_usuario`, `username`, `password`, `estado`, `es_admin_supremo`, `id_rol`, `id_empleado`) VALUES
  (1, 'admin',  '$2a$10$bvPfaGRFA8sLyulWsWQBf.4ZW4HFNekKTQICG/gzYw35HVcraEJPC', 1, 1, 1, 1),
  (2, 'cajero', '$2a$10$v2Gy1f/fkwyWtIzzPSeFEumyO1CjJveWNrkAgf83yfllPiuozKa4O', 1, 0, 2, 2),
  (3, 'cocina', '$2a$10$pm8rF5t7SJjE14mPoso6xO0GlhB4OmzILmnAlCi2lqL5D8B4wGqGe', 1, 0, 3, 3);

-- Clientes
INSERT INTO `cliente` (`id_cliente`, `nombre`, `telefono`) VALUES
  (1, 'Carlos', '999111222'),
  (2, 'Andrea', '988222333'),
  (3, 'Miguel', '977333444');

-- Categorias
INSERT INTO `categoria` (`id_categoria`, `nombre`) VALUES
  (1, 'Pizzas'),
  (2, 'Bebidas'),
  (3, 'Combos'),
  (4, 'Panizzas'),
  (5, 'Complementos');

-- Medidas
INSERT INTO `medida` (`id_medida`, `descripcion`, `sigla`) VALUES
  (1, 'Kilogramo', 'kg'),
  (2, 'Litro', 'L'),
  (3, 'Unidad', 'UND');

-- Productos (catalogo Mamma Tomato)
INSERT INTO `producto` (`id_producto`, `codigo`, `nombre`, `precio`, `stock`, `tamanio`, `disponible`, `id_categoria`) VALUES
  (1, 'PZ0001', 'Americana Di Roma', 49.90, NULL, 'familiar', 1, 1),
  (2, 'PZ0002', 'Diavola Pepperoni', 49.90, NULL, 'familiar', 1, 1),
  (3, 'BB0001', 'Coca Cola 500ml',    7.90,   24, NULL,       1, 2),
  (4, 'BB0002', 'Agua mineral',       3.00,   18, NULL,       1, 2),
  (5, 'CB0001', 'Trio Fiesta',       79.90, NULL, NULL,       1, 3),
  (6,  'PZ0003', 'Americana Bondiola',    52.90, NULL, 'familiar', 1, 1),
  (7,  'PZ0004', 'Americana Prosciutto',  54.90, NULL, 'familiar', 1, 1),
  (8,  'PZ0005', '4 Stagioni',            56.90, NULL, 'familiar', 1, 1),
  (9,  'PZ0006', 'Prosciutto Portobello', 59.90, NULL, 'familiar', 1, 1),
  (10, 'PZ0007', 'Prosciutto & Carne',    59.90, NULL, 'familiar', 1, 1),
  (11, 'PZ0008', 'Tutto Carnes',          58.90, NULL, 'familiar', 1, 1),
  (12, 'PZ0009', 'Ahumado 3 Carnes',      56.90, NULL, 'familiar', 1, 1),
  (13, 'PZ0010', 'Supremissima',          54.90, NULL, 'familiar', 1, 1),
  (14, 'PZ0011', 'Hawaiiana Fiesta',      52.90, NULL, 'familiar', 1, 1),
  (15, 'PZ0012', 'Hawaiiana Crispy',      56.90, NULL, 'familiar', 1, 1),
  (16, 'PZ0013', 'Miss Veggie',           54.90, NULL, 'familiar', 1, 1),
  (17, 'PZ0014', 'Caprichosa',            58.90, NULL, 'familiar', 1, 1),
  (18, 'PZ0015', 'Carbonara',             54.90, NULL, 'familiar', 1, 1),
  (19, 'PZ0016', 'Quattro Formaggi',      54.90, NULL, 'familiar', 1, 1),
  (20, 'PZ0017', 'Diavola Pepperoni',     32.90, NULL, 'mediano', 1, 1),
  (21, 'PZ0018', 'Americana Di Roma',     32.90, NULL, 'mediano', 1, 1),
  (22, 'PZ0019', '4 Stagioni',            39.90, NULL, 'mediano', 1, 1),
  (23, 'PZ0020', 'Prosciutto Portobello', 42.90, NULL, 'mediano', 1, 1),
  (24, 'PZ0021', 'Prosciutto & Carne',    42.90, NULL, 'mediano', 1, 1),
  (25, 'PZ0022', 'Supremissima',          37.90, NULL, 'mediano', 1, 1),
  (26, 'PZ0023', 'Hawaiiana Fiesta',      35.90, NULL, 'mediano', 1, 1),
  (27, 'PZ0024', 'Hawaiiana Crispy',      39.90, NULL, 'mediano', 1, 1),
  (28, 'PZ0025', 'Miss Veggie',           37.90, NULL, 'mediano', 1, 1),
  (29, 'PZ0026', 'Caprichosa',            41.90, NULL, 'mediano', 1, 1),
  (30, 'PZ0027', 'Carbonara',             37.90, NULL, 'mediano', 1, 1),
  (31, 'PZ0028', 'Quattro Formaggi',      37.90, NULL, 'mediano', 1, 1),
  (32, 'MN0001', 'Menu Diavola Pepperoni', 19.90, NULL, 'personal', 1, 1),
  (33, 'MN0002', 'Menu Americana Di Roma', 19.90, NULL, 'personal', 1, 1),
  (34, 'MN0003', 'Menu Hawaiiana Fiesta',  19.90, NULL, 'personal', 1, 1),
  (35, 'MN0004', 'Menu Supremissima',      19.90, NULL, 'personal', 1, 1),
  (36, 'PA0001', 'Panizza Crispy Chicken',    18.90, NULL, NULL, 1, 4),
  (37, 'PA0002', 'Panizza Hawaiana',          20.90, NULL, NULL, 1, 4),
  (38, 'PA0003', 'Panizza Bondiola Glaseada', 20.90, NULL, NULL, 1, 4),
  (39, 'BB0003', 'Pepsi 355ml',     6.90, 24, NULL, 1, 2),
  (40, 'BB0004', 'Pepsi 750ml',     9.90, 18, NULL, 1, 2),
  (41, 'BB0005', 'Inca Kola 500ml', 7.90, 24, NULL, 1, 2),
  (42, 'CP0001', 'Fugazza Especial al Ajo', 14.90, NULL, NULL, 1, 5),
  (43, 'CP0002', 'Fugazza Champinones',     14.90, NULL, NULL, 1, 5),
  (44, 'CP0003', 'Fugazza Cebolla',         14.90, NULL, NULL, 1, 5),
  (45, 'CP0004', 'Fugazza Aceitunas',       14.90, NULL, NULL, 1, 5),
  (46, 'CP0005', 'Crema Alioli',             2.90, 40, NULL, 1, 5),
  (47, 'CP0006', 'Crema Mediterranea',       2.90, 40, NULL, 1, 5),
  (48, 'CB0002', 'Gran Dupla XL - Supremissima + Americana Di Roma',     59.90, NULL, NULL, 1, 3),
  (49, 'CB0003', 'Gran Dupla XL - Hawaiiana Fiesta + Americana Di Roma', 49.90, NULL, NULL, 1, 3),
  (50, 'CB0004', 'Plan Perfecto - 4 Stagioni',         49.90, NULL, NULL, 1, 3),
  (51, 'CB0005', 'Plan Perfecto - Hawaiiana',          39.90, NULL, NULL, 1, 3),
  (52, 'CB0006', 'Plan Perfecto - Supremissima',       39.90, NULL, NULL, 1, 3),
  (53, 'CB0007', 'Plan Perfecto - Diavola Pepperoni',  39.90, NULL, NULL, 1, 3),
  (54, 'CB0008', 'Plan Perfecto - Americana Di Roma',  39.90, NULL, NULL, 1, 3),
  (55, 'CB0009', 'Mamma Mia Americana Bondiola',       34.90, NULL, NULL, 1, 3),
  (56, 'CB0010', 'Mamma Mia Hawaiiana Fiesta',         29.90, NULL, NULL, 1, 3),
  (57, 'CB0011', 'Mamma Mia Diavola Pepperoni',        29.90, NULL, NULL, 1, 3),
  (58, 'CB0012', 'Mamma Mia Americana Prosciutto',     39.90, NULL, NULL, 1, 3);

-- Insumos
INSERT INTO `insumo` (`id_insumo`, `codigo`, `nombre`, `precio`, `estado`, `stock`, `cantidad_minima`, `id_medida`) VALUES
  (1, 'IN0001', 'Queso mozzarella', 18.00, 'normal',  4.500, 3.000, 1),
  (2, 'IN0002', 'Salsa de tomate',   9.50, 'normal',  6.000, 2.000, 2),
  (3, 'IN0003', 'Masa familiar',     2.20, 'normal', 18.000, 8.000, 3),
  (4, 'IN0004', 'Pepperoni',        28.00, 'normal',  2.200, 1.500, 1),
  (5, 'IN0005', 'Gaseosa 500ml',     2.20, 'normal', 24.000, 8.000, 3),
  (6, 'IN0006', 'Agua mineral',      1.60, 'normal', 18.000, 6.000, 3);

-- Metodos de pago
INSERT INTO `metodo_pago` (`id_metodopago`, `descripcion`, `activo`) VALUES
  (1, 'Efectivo', 1),
  (2, 'Tarjeta', 1),
  (3, 'Yape', 1),
  (4, 'Plin', 1);

-- Tipos de movimiento
INSERT INTO `tipo_movimiento` (`id_tipomovimiento`, `descripcion`, `operacion`) VALUES
  (1, 'Compra', 'Entrada'),
  (2, 'Venta', 'Salida'),
  (3, 'Merma', 'Salida'),
  (4, 'Ajuste', 'Entrada');

-- Proveedores
INSERT INTO `proveedor` (`id_proveedor`, `ruc`, `nombre`, `telefono`, `direccion`) VALUES
  (1, '20123456789', 'Distribuidora Andina', '955111333', 'Av. Central 456'),
  (2, '20987654321', 'Lacteos del Sur',      '944222111', 'Jr. Lima 120');

-- Recetas (producto_insumo)
INSERT INTO `producto_insumo` (`cantidad`, `id_insumo`, `id_producto`) VALUES
  (0.500, 1, 1),
  (0.250, 2, 1),
  (1.000, 3, 1),
  (0.450, 1, 2),
  (0.200, 2, 2),
  (1.000, 3, 2),
  (0.250, 4, 2),
  (1.000, 5, 3),
  (1.000, 6, 4);

-- Combos (combo_producto)
INSERT INTO `combo_producto` (`cantidad`, `id_producto`, `id_combo`) VALUES
  (1, 1, 5),
  (2, 3, 5);

-- Promociones
INSERT INTO `promocion` (`id_promocion`, `nombre`, `descripcion`, `tipo_descuento`, `valor_descuento`, `activa`) VALUES
  (1, 'Bebida promo', '10% en gaseosa', 'Porcentaje', 10.00, 1),
  (2, 'Pizza lunes',  'S/ 5 menos',     'Monto',       5.00, 0);

INSERT INTO `promocion_producto` (`id_promocion`, `id_producto`, `cantidad_minima`) VALUES
  (1, 3, 1),
  (2, 1, 1);

-- Pedidos
INSERT INTO `pedido` (`id_pedido`, `fecha`, `estado`, `motivo_anulacion`, `id_usuario`, `id_cliente`) VALUES
  (1, '2026-05-29 11:20:00', 'PENDIENTE',  NULL, 2, 1),
  (2, '2026-05-29 11:35:00', 'PREPARANDO', NULL, 2, 2),
  (3, '2026-05-29 10:50:00', 'ATENDIDO',   NULL, 2, 3),
  (4, '2026-05-28 20:10:00', 'ANULADO',    'Cliente solicito cancelacion', 2, 1);

-- Detalle de pedidos
INSERT INTO `detalle_pedido` (`id_detallepedido`, `cantidad`, `precio_unitario`, `subtotal`, `descuento`, `observacion`, `id_pedido`, `id_producto`) VALUES
  (1, 1, 35.00, 35.00, 0.00, 'Sin cebolla',  1, 1),
  (2, 2,  4.00,  7.20, 0.80, NULL,            1, 3),
  (3, 1, 38.00, 38.00, 0.00, 'Masa delgada',  2, 2),
  (4, 1, 45.00, 45.00, 0.00, NULL,            3, 5),
  (5, 1, 35.00, 35.00, 0.00, NULL,            4, 1);

-- Boletas
INSERT INTO `boleta` (`id_boleta`, `subtotal`, `igv`, `total`, `id_metodopago`, `id_pedido`) VALUES
  (1, 42.20, 7.60, 49.80, 1, 1),
  (2, 38.00, 6.84, 44.84, 3, 2),
  (3, 45.00, 8.10, 53.10, 2, 3),
  (4, 35.00, 6.30, 41.30, 1, 4);

-- Pagos (desglose por boleta; el pedido 3 ejemplifica un pago mixto)
INSERT INTO `pago` (`id_pago`, `monto`, `id_pedido`, `id_metodopago`) VALUES
  (1, 49.80, 1, 1),
  (2, 44.84, 2, 3),
  (3, 30.00, 3, 2),
  (4, 23.10, 3, 1),
  (5, 41.30, 4, 1);

-- Compras
INSERT INTO `compra` (`id_compra`, `fecha`, `total`, `estado`, `id_proveedor`, `id_usuario`) VALUES
  (1, '2026-05-28', 185.50, 'Registrada', 1, 1),
  (2, '2026-05-27', 240.00, 'Registrada', 2, 1);

-- Movimientos
INSERT INTO `movimiento` (`id_movimiento`, `documento`, `fecha`, `glosa`, `id_tipomovimiento`, `id_compra`, `id_usuario`) VALUES
  (1, 'C-0001', '2026-05-28', 'Ingreso por compra', 1, 1,    1),
  (2, 'P-0003', '2026-05-29', 'Consumo por venta',  2, NULL, 2),
  (3, 'M-0001', '2026-05-29', 'Merma salsa',        3, NULL, 1);

-- Detalle de movimientos
INSERT INTO `detalle_movimiento` (`id_detallemovimiento`, `cantidad`, `stock_resultante`, `id_insumo`, `id_movimiento`) VALUES
  (1,  5.000,  8.500, 1, 1),
  (2, 24.000, 30.000, 5, 1),
  (3,  0.500,  4.500, 1, 2),
  (4,  0.250,  6.000, 2, 2),
  (5,  0.500,  5.500, 2, 3);
