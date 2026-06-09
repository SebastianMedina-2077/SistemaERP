import { db } from "./data.js";

const IGV = 0.18;
const LOGIN_URL = new URL("../login.html", import.meta.url).href;

const byId = (items, key, value) => items.find((item) => item[key] === value);
const money = (value) => Number(value || 0).toFixed(2);

export const format = {
  money(value) {
    return `S/ ${money(value)}`;
  },
  badge(text) {
    const normalized = String(text).toLowerCase();
    if (["activo", "atendido", "registrada", "entrada", "disponible"].some((word) => normalized.includes(word))) return "badge badge-success";
    if (["anulado", "inactivo", "salida", "no disponible"].some((word) => normalized.includes(word))) return "badge badge-danger";
    if (["pendiente", "bajo"].some((word) => normalized.includes(word))) return "badge badge-warning";
    return "badge badge-info";
  }
};

export const AuthService = {
  login(username, password) {
    const user = db.usuarios.find((item) => item.username === username && item.password === password && item.estado);
    if (!user) return null;
    const rol = byId(db.roles, "idRol", user.idRol);
    const empleado = byId(db.empleados, "idEmpleado", user.idEmpleado);
    return {
      idUsuario: user.idUsuario,
      username: user.username,
      rol: rol.nombre,
      nombre: empleado ? `${empleado.nombre} ${empleado.apellido}` : user.username
    };
  },
  getSession(fallbackRole = "Administrador") {
    const session = sessionStorage.getItem("erpUser");
    if (session) return JSON.parse(session);
    return { username: fallbackRole.toLowerCase(), rol: fallbackRole, nombre: fallbackRole };
  },
  setSession(user) {
    sessionStorage.setItem("erpUser", JSON.stringify(user));
  },
  logout() {
    sessionStorage.removeItem("erpUser");
    window.location.href = LOGIN_URL;
  }
};

export const CatalogService = {
  getCategorias: () => db.categorias,
  getProductos: () => db.productos,
  getProductosDisponibles: () => db.productos.filter((item) => item.disponible),
  getProducto(idProducto) {
    return byId(db.productos, "idProducto", idProducto);
  },
  getCategoria(idCategoria) {
    return byId(db.categorias, "idCategoria", idCategoria);
  },
  getRecetas() {
    return db.productos.map((producto) => ({
      producto,
      items: db.productoInsumo
        .filter((item) => item.idProducto === producto.idProducto)
        .map((item) => ({
          ...item,
          insumo: InventoryService.getInsumo(item.idInsumo)
        }))
    })).filter((recipe) => recipe.items.length);
  },
  getCombos() {
    return db.comboProducto.reduce((acc, item) => {
      const combo = this.getProducto(item.idCombo);
      const producto = this.getProducto(item.idProducto);
      const found = acc.find((entry) => entry.combo.idProducto === item.idCombo);
      if (found) found.items.push({ producto, cantidad: item.cantidad });
      else acc.push({ combo, items: [{ producto, cantidad: item.cantidad }] });
      return acc;
    }, []);
  },
  getPromociones: () => db.promociones,
  getPromotionForProduct(idProducto) {
    return db.promociones.find((promo) => promo.activa && promo.productos.includes(idProducto));
  },
  getDiscount(product, quantity = 1) {
    const promo = this.getPromotionForProduct(product.idProducto);
    if (!promo) return 0;
    const base = product.precio * quantity;
    return promo.tipoDescuento === "Porcentaje" ? base * (promo.valorDescuento / 100) : promo.valorDescuento * quantity;
  }
};

export const PeopleService = {
  getRoles: () => db.roles,
  getEmpleados: () => db.empleados,
  getUsuarios() {
    return db.usuarios.map((user) => ({
      ...user,
      rol: byId(db.roles, "idRol", user.idRol),
      empleado: byId(db.empleados, "idEmpleado", user.idEmpleado)
    }));
  },
  getProveedores: () => db.proveedores,
  getUsuario(idUsuario) {
    const user = byId(db.usuarios, "idUsuario", idUsuario);
    const empleado = user ? byId(db.empleados, "idEmpleado", user.idEmpleado) : null;
    return user ? { ...user, empleado } : null;
  }
};

export const InventoryService = {
  getMedidas: () => db.medidas,
  getInsumos: () => db.insumos,
  getInsumo(idInsumo) {
    return byId(db.insumos, "idInsumo", idInsumo);
  },
  getMedida(idMedida) {
    return byId(db.medidas, "idMedida", idMedida);
  },
  getCompras() {
    return db.compras.map((compra) => ({
      ...compra,
      proveedor: byId(db.proveedores, "idProveedor", compra.idProveedor),
      usuario: PeopleService.getUsuario(compra.idUsuario)
    }));
  },
  getMovimientos() {
    return db.movimientos.map((movimiento) => ({
      ...movimiento,
      tipo: byId(db.tiposMovimiento, "idTipoMovimiento", movimiento.idTipoMovimiento),
      usuario: PeopleService.getUsuario(movimiento.idUsuario)
    }));
  },
  getKardex() {
    return db.detalleMovimiento.map((detalle) => {
      const movimiento = byId(db.movimientos, "idMovimiento", detalle.idMovimiento);
      const tipo = byId(db.tiposMovimiento, "idTipoMovimiento", movimiento.idTipoMovimiento);
      const insumo = this.getInsumo(detalle.idInsumo);
      return {
        ...detalle,
        movimiento,
        tipo,
        insumo,
        entrada: tipo.operacion === "Entrada" ? detalle.cantidad : 0,
        salida: tipo.operacion === "Salida" ? detalle.cantidad : 0
      };
    });
  },
  getLowStock() {
    return db.insumos.filter((item) => item.stock <= item.stockMinimo);
  },
  checkProductStock(product, quantity = 1) {
    const composition = db.productoInsumo.filter((item) => item.idProducto === product.idProducto);
    if (!composition.length) return { ok: true };
    const missing = composition
      .map((item) => {
        const supply = this.getInsumo(item.idInsumo);
        return { supply, required: item.cantidad * quantity };
      })
      .filter((item) => item.supply.stock < item.required);
    return { ok: missing.length === 0, missing };
  }
};

export const SalesService = {
  getPedidos() {
    return db.pedidos.map((pedido) => {
      const cliente = byId(db.clientes, "idCliente", pedido.idCliente);
      const usuario = PeopleService.getUsuario(pedido.idUsuario);
      const ticket = byId(db.boletas, "idPedido", pedido.idPedido);
      const boleta = ticket ? { ...ticket, metodo: byId(db.metodosPago, "idMetodoPago", ticket.idMetodoPago) } : null;
      return { ...pedido, cliente, usuario, boleta, detalles: this.getDetallePedido(pedido.idPedido) };
    });
  },
  getDetallePedido(idPedido) {
    return db.detallePedido
      .filter((detalle) => detalle.idPedido === idPedido)
      .map((detalle) => ({ ...detalle, producto: CatalogService.getProducto(detalle.idProducto) }));
  },
  getClientes() {
    return db.clientes.map((cliente) => ({
      ...cliente,
      pedidos: db.pedidos.filter((pedido) => pedido.idCliente === cliente.idCliente).length
    }));
  },
  getBoletas() {
    return db.boletas.map((boleta) => ({
      ...boleta,
      metodo: byId(db.metodosPago, "idMetodoPago", boleta.idMetodoPago),
      pedido: byId(db.pedidos, "idPedido", boleta.idPedido)
    }));
  },
  getMetodosPago: () => db.metodosPago,
  getKitchenOrders() {
    return this.getPedidos()
      .filter((pedido) => ["Pendiente", "Preparando", "Atendido"].includes(pedido.estado))
      .sort((a, b) => a.fecha.localeCompare(b.fecha));
  },
  updateOrderStatus(idPedido, estado) {
    const pedido = byId(db.pedidos, "idPedido", idPedido);
    if (pedido) pedido.estado = estado;
    return pedido;
  },
  createDraftOrder({ customerName, customerPhone, paymentMethod, items }) {
    const subtotal = items.reduce((sum, item) => sum + item.subtotal, 0);
    const igv = subtotal * IGV;
    return {
      numeroBoleta: `B001-${String(db.boletas.length + 1).padStart(6, "0")}`,
      cliente: { nombre: customerName, telefono: customerPhone },
      metodoPago: paymentMethod,
      subtotal,
      igv,
      total: subtotal + igv,
      items
    };
  },
  getTopProducts() {
    const totals = db.detallePedido.reduce((acc, item) => {
      acc[item.idProducto] = (acc[item.idProducto] || 0) + item.cantidad;
      return acc;
    }, {});
    return Object.entries(totals)
      .map(([idProducto, cantidad]) => ({ producto: CatalogService.getProducto(Number(idProducto)), cantidad }))
      .sort((a, b) => b.cantidad - a.cantidad);
  }
};

export const ReportService = {
  getDashboardStats() {
    const pedidos = SalesService.getPedidos();
    const boletas = SalesService.getBoletas().filter((item) => item.pedido.estado !== "Anulado");
    return [
      { label: "Ventas del dia", value: format.money(boletas.reduce((sum, item) => sum + item.total, 0)) },
      { label: "Pedidos pendientes", value: pedidos.filter((item) => item.estado === "Pendiente").length },
      { label: "Insumos bajo stock", value: InventoryService.getLowStock().length },
      { label: "Pedidos anulados", value: pedidos.filter((item) => item.estado === "Anulado").length }
    ];
  },
  getSalesStats() {
    const pedidos = SalesService.getPedidos();
    const active = pedidos.filter((item) => item.estado !== "Anulado");
    const total = active.reduce((sum, item) => sum + (item.boleta?.total || 0), 0);
    return [
      { label: "Ingresos", value: format.money(total) },
      { label: "Pedidos validos", value: active.length },
      { label: "Ticket promedio", value: format.money(active.length ? total / active.length : 0) },
      { label: "Anulados", value: pedidos.filter((item) => item.estado === "Anulado").length }
    ];
  }
};

export const ApiPreview = {
  baseUrl: "/api",
  endpoints: [
    "GET /api/productos",
    "POST /api/productos",
    "GET /api/pedidos",
    "POST /api/pedidos",
    "PATCH /api/pedidos/{id}/estado",
    "GET /api/insumos",
    "POST /api/compras",
    "GET /api/kardex"
  ]
};
