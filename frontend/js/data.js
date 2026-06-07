export const db = {
  roles: [
    { idRol: 1, nombre: "Administrador" },
    { idRol: 2, nombre: "Cajero" },
    { idRol: 3, nombre: "Cocina" }
  ],
  empleados: [
    { idEmpleado: 1, nombre: "Lucia", apellido: "Ramos", dni: "72814391", telefono: "987654321", cargo: "Administrador" },
    { idEmpleado: 2, nombre: "Mario", apellido: "Salas", dni: "73451298", telefono: "976431258", cargo: "Cajero" },
    { idEmpleado: 3, nombre: "Rosa", apellido: "Vega", dni: "70234511", telefono: "965214783", cargo: "Cocina" }
  ],
  usuarios: [
    { idUsuario: 1, username: "admin", password: "admin123", estado: true, idRol: 1, idEmpleado: 1 },
    { idUsuario: 2, username: "cajero", password: "cajero123", estado: true, idRol: 2, idEmpleado: 2 },
    { idUsuario: 3, username: "cocina", password: "cocina123", estado: true, idRol: 3, idEmpleado: 3 }
  ],
  clientes: [
    { idCliente: 1, nombre: "Carlos", telefono: "999111222" },
    { idCliente: 2, nombre: "Andrea", telefono: "988222333" },
    { idCliente: 3, nombre: "Miguel", telefono: "977333444" }
  ],
  categorias: [
    { idCategoria: 1, nombre: "Pizzas" },
    { idCategoria: 2, nombre: "Bebidas" },
    { idCategoria: 3, nombre: "Combos" }
  ],
  productos: [
    { idProducto: 1, codigo: "PZ0001", nombre: "Pizza Americana", precio: 35.00, stock: null, tamanio: "familiar", disponible: true, idCategoria: 1 },
    { idProducto: 2, codigo: "PZ0002", nombre: "Pizza Pepperoni", precio: 38.00, stock: null, tamanio: "familiar", disponible: true, idCategoria: 1 },
    { idProducto: 3, codigo: "BB0001", nombre: "Gaseosa 500ml", precio: 4.00, stock: 24, tamanio: null, disponible: true, idCategoria: 2 },
    { idProducto: 4, codigo: "BB0002", nombre: "Agua mineral", precio: 3.00, stock: 18, tamanio: null, disponible: true, idCategoria: 2 },
    { idProducto: 5, codigo: "CB0001", nombre: "Combo Familiar", precio: 45.00, stock: null, tamanio: null, disponible: true, idCategoria: 3 }
  ],
  medidas: [
    { idMedida: 1, descripcion: "Kilogramo", sigla: "kg" },
    { idMedida: 2, descripcion: "Litro", sigla: "L" },
    { idMedida: 3, descripcion: "Unidad", sigla: "UND" }
  ],
  insumos: [
    { idInsumo: 1, codigo: "IN0001", nombre: "Queso mozzarella", precio: 18.00, stock: 4.5, stockMinimo: 3, idMedida: 1 },
    { idInsumo: 2, codigo: "IN0002", nombre: "Salsa de tomate", precio: 9.50, stock: 6, stockMinimo: 2, idMedida: 2 },
    { idInsumo: 3, codigo: "IN0003", nombre: "Masa familiar", precio: 2.20, stock: 18, stockMinimo: 8, idMedida: 3 },
    { idInsumo: 4, codigo: "IN0004", nombre: "Pepperoni", precio: 28.00, stock: 2.2, stockMinimo: 1.5, idMedida: 1 },
    { idInsumo: 5, codigo: "IN0005", nombre: "Gaseosa 500ml", precio: 2.20, stock: 24, stockMinimo: 8, idMedida: 3 },
    { idInsumo: 6, codigo: "IN0006", nombre: "Agua mineral", precio: 1.60, stock: 18, stockMinimo: 6, idMedida: 3 }
  ],
  productoInsumo: [
    { idProducto: 1, idInsumo: 1, cantidad: 0.5 },
    { idProducto: 1, idInsumo: 2, cantidad: 0.25 },
    { idProducto: 1, idInsumo: 3, cantidad: 1 },
    { idProducto: 2, idInsumo: 1, cantidad: 0.45 },
    { idProducto: 2, idInsumo: 2, cantidad: 0.2 },
    { idProducto: 2, idInsumo: 3, cantidad: 1 },
    { idProducto: 2, idInsumo: 4, cantidad: 0.25 },
    { idProducto: 3, idInsumo: 5, cantidad: 1 },
    { idProducto: 4, idInsumo: 6, cantidad: 1 }
  ],
  comboProducto: [
    { idCombo: 5, idProducto: 1, cantidad: 1 },
    { idCombo: 5, idProducto: 3, cantidad: 2 }
  ],
  promociones: [
    { idPromocion: 1, nombre: "Bebida promo", descripcion: "10% en gaseosa", tipoDescuento: "Porcentaje", valorDescuento: 10, activa: true, productos: [3] },
    { idPromocion: 2, nombre: "Pizza lunes", descripcion: "S/ 5 menos", tipoDescuento: "Monto", valorDescuento: 5, activa: false, productos: [1] }
  ],
  metodosPago: [
    { idMetodoPago: 1, descripcion: "Efectivo", activo: true },
    { idMetodoPago: 2, descripcion: "Tarjeta", activo: true },
    { idMetodoPago: 3, descripcion: "Yape", activo: true },
    { idMetodoPago: 4, descripcion: "Plin", activo: true }
  ],
  pedidos: [
    { idPedido: 1, fecha: "2026-05-29 11:20", estado: "Pendiente", idUsuario: 2, idCliente: 1 },
    { idPedido: 2, fecha: "2026-05-29 11:35", estado: "Preparando", idUsuario: 2, idCliente: 2 },
    { idPedido: 3, fecha: "2026-05-29 10:50", estado: "Atendido", idUsuario: 2, idCliente: 3 },
    { idPedido: 4, fecha: "2026-05-28 20:10", estado: "Anulado", idUsuario: 2, idCliente: 1, motivoAnulacion: "Cliente solicito cancelacion" }
  ],
  detallePedido: [
    { idDetallePedido: 1, idPedido: 1, idProducto: 1, cantidad: 1, precioUnitario: 35, descuento: 0, subtotal: 35, observacion: "Sin cebolla" },
    { idDetallePedido: 2, idPedido: 1, idProducto: 3, cantidad: 2, precioUnitario: 4, descuento: 0.8, subtotal: 7.2, observacion: "" },
    { idDetallePedido: 3, idPedido: 2, idProducto: 2, cantidad: 1, precioUnitario: 38, descuento: 0, subtotal: 38, observacion: "Masa delgada" },
    { idDetallePedido: 4, idPedido: 3, idProducto: 5, cantidad: 1, precioUnitario: 45, descuento: 0, subtotal: 45, observacion: "" },
    { idDetallePedido: 5, idPedido: 4, idProducto: 1, cantidad: 1, precioUnitario: 35, descuento: 0, subtotal: 35, observacion: "" }
  ],
  boletas: [
    { idBoleta: 1, numeroBoleta: "B001-000001", subtotal: 42.20, igv: 7.60, total: 49.80, idMetodoPago: 1, idPedido: 1 },
    { idBoleta: 2, numeroBoleta: "B001-000002", subtotal: 38.00, igv: 6.84, total: 44.84, idMetodoPago: 3, idPedido: 2 },
    { idBoleta: 3, numeroBoleta: "B001-000003", subtotal: 45.00, igv: 8.10, total: 53.10, idMetodoPago: 2, idPedido: 3 },
    { idBoleta: 4, numeroBoleta: "B001-000004", subtotal: 35.00, igv: 6.30, total: 41.30, idMetodoPago: 1, idPedido: 4 }
  ],
  proveedores: [
    { idProveedor: 1, ruc: "20123456789", nombre: "Distribuidora Andina", telefono: "955111333", direccion: "Av. Central 456" },
    { idProveedor: 2, ruc: "20987654321", nombre: "Lacteos del Sur", telefono: "944222111", direccion: "Jr. Lima 120" }
  ],
  compras: [
    { idCompra: 1, fecha: "2026-05-28", total: 185.50, estado: "Registrada", idProveedor: 1, idUsuario: 1 },
    { idCompra: 2, fecha: "2026-05-27", total: 240.00, estado: "Registrada", idProveedor: 2, idUsuario: 1 }
  ],
  tiposMovimiento: [
    { idTipoMovimiento: 1, descripcion: "Compra", operacion: "Entrada" },
    { idTipoMovimiento: 2, descripcion: "Venta", operacion: "Salida" },
    { idTipoMovimiento: 3, descripcion: "Merma", operacion: "Salida" },
    { idTipoMovimiento: 4, descripcion: "Ajuste", operacion: "Entrada" }
  ],
  movimientos: [
    { idMovimiento: 1, documento: "C-0001", fecha: "2026-05-28", glosa: "Ingreso por compra", idTipoMovimiento: 1, idCompra: 1, idUsuario: 1 },
    { idMovimiento: 2, documento: "P-0003", fecha: "2026-05-29", glosa: "Consumo por venta", idTipoMovimiento: 2, idCompra: null, idUsuario: 2 },
    { idMovimiento: 3, documento: "M-0001", fecha: "2026-05-29", glosa: "Merma salsa", idTipoMovimiento: 3, idCompra: null, idUsuario: 1 }
  ],
  detalleMovimiento: [
    { idDetalleMovimiento: 1, idMovimiento: 1, idInsumo: 1, cantidad: 5, stockResultante: 8.5 },
    { idDetalleMovimiento: 2, idMovimiento: 1, idInsumo: 5, cantidad: 24, stockResultante: 30 },
    { idDetalleMovimiento: 3, idMovimiento: 2, idInsumo: 1, cantidad: 0.5, stockResultante: 4.5 },
    { idDetalleMovimiento: 4, idMovimiento: 2, idInsumo: 2, cantidad: 0.25, stockResultante: 6 },
    { idDetalleMovimiento: 5, idMovimiento: 3, idInsumo: 2, cantidad: 0.5, stockResultante: 5.5 }
  ]
};
