package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.BoletaDTO;
import com.erp.pizzeria.dto.CotizacionDTO;
import com.erp.pizzeria.dto.DetallePedidoDTO;
import com.erp.pizzeria.dto.PagoDTO;
import com.erp.pizzeria.dto.PedidoDTO;
import com.erp.pizzeria.dto.TiendaCatalogoDTO;
import com.erp.pizzeria.dto.TiendaCotizacionDTO;
import com.erp.pizzeria.dto.TiendaEntregaDTO;
import com.erp.pizzeria.dto.TiendaPagoDTO;
import com.erp.pizzeria.dto.TiendaPedidoCreadoDTO;
import com.erp.pizzeria.dto.TiendaPedidoDTO;
import com.erp.pizzeria.dto.TiendaPedidoResumenDTO;
import com.erp.pizzeria.dto.TiendaPromoValidacionResponseDTO;
import com.erp.pizzeria.dto.TiendaTarjetaDTO;
import com.erp.pizzeria.model.ClienteCuenta;
import com.erp.pizzeria.model.ClienteTarjeta;
import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.MetodoPago;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.PedidoWeb;
import com.erp.pizzeria.model.Promocion;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.ModalidadEntrega;
import com.erp.pizzeria.repository.ClienteTarjetaRepository;
import com.erp.pizzeria.repository.MetodoPagoRepository;
import com.erp.pizzeria.repository.PedidoWebRepository;
import com.erp.pizzeria.repository.PromocionRepository;
import com.erp.pizzeria.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pedidos de la tienda en linea. No duplica la logica de venta: re-cotiza y
 * crea el pedido con el MISMO flujo del POS ({@link PedidoService}), que
 * descuenta stock, genera la boleta con correlativo y avisa a cocina por SSE.
 * Aqui solo se resuelve lo propio del canal web: cuenta del cliente, entrega,
 * metodos de pago por nombre y tarjetas guardadas.
 */
@Service
@Transactional(readOnly = true)
public class TiendaPedidoService {

    /** La suma de los pagos no coincide con el total cotizado (el controlador lo traduce a 400). */
    public static class PagosNoCuadranException extends RuntimeException {
        private final BigDecimal total;
        private final BigDecimal pagado;

        public PagosNoCuadranException(BigDecimal total, BigDecimal pagado) {
            super("Los pagos no cuadran con el total");
            this.total = total;
            this.pagado = pagado;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public BigDecimal getPagado() {
            return pagado;
        }
    }

    /** La tarjeta guardada no existe o pertenece a otra cuenta (el controlador lo traduce a 403). */
    public static class TarjetaNoAutorizadaException extends RuntimeException {
        public TarjetaNoAutorizadaException(String mensaje) {
            super(mensaje);
        }
    }

    /** Username del usuario sistema (deshabilitado) que firma los pedidos web. */
    private static final String USUARIO_TIENDA = "tienda";
    private static final String METODO_TARJETA = "TARJETA";

    private final PedidoService pedidoService;
    private final CatalogService catalogService;
    private final TiendaCuentaService cuentaService;
    private final MetodoPagoRepository metodoPagoRepository;
    private final ClienteTarjetaRepository tarjetaRepository;
    private final PedidoWebRepository pedidoWebRepository;
    private final UsuarioRepository usuarioRepository;
    private final PromocionRepository promocionRepository;
    private final com.erp.pizzeria.repository.ProductoAdicionalRepository productoAdicionalRepository;

    public TiendaPedidoService(PedidoService pedidoService,
                               CatalogService catalogService,
                               TiendaCuentaService cuentaService,
                               MetodoPagoRepository metodoPagoRepository,
                               ClienteTarjetaRepository tarjetaRepository,
                               PedidoWebRepository pedidoWebRepository,
                               UsuarioRepository usuarioRepository,
                               PromocionRepository promocionRepository,
                               com.erp.pizzeria.repository.ProductoAdicionalRepository productoAdicionalRepository) {
        this.pedidoService = pedidoService;
        this.catalogService = catalogService;
        this.cuentaService = cuentaService;
        this.metodoPagoRepository = metodoPagoRepository;
        this.tarjetaRepository = tarjetaRepository;
        this.pedidoWebRepository = pedidoWebRepository;
        this.usuarioRepository = usuarioRepository;
        this.promocionRepository = promocionRepository;
        this.productoAdicionalRepository = productoAdicionalRepository;
    }

    /** Detalle de un producto con sus adicionales disponibles (para la pagina de personalizacion). */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.erp.pizzeria.dto.TiendaProductoDetalleDTO detalleProducto(Integer idProducto) {
        com.erp.pizzeria.model.Producto p = catalogService.getProducto(idProducto);
        java.util.List<com.erp.pizzeria.dto.TiendaProductoDetalleDTO.AdicionalItem> adicionales =
                productoAdicionalRepository.findByProducto_IdProducto(idProducto).stream()
                        .map(com.erp.pizzeria.model.ProductoAdicional::getAdicional)
                        .filter(a -> Boolean.TRUE.equals(a.getDisponible()))
                        .map(a -> new com.erp.pizzeria.dto.TiendaProductoDetalleDTO.AdicionalItem(
                                a.getIdAdicional(), a.getNombre(), a.getPrecio(), a.getGrupo()))
                        .toList();
        return new com.erp.pizzeria.dto.TiendaProductoDetalleDTO(
                p.getIdProducto(), p.getNombre(), p.getPrecio(),
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getImagenUrl(), adicionales);
    }

    /** Catalogo publico: solo productos disponibles y promociones activas. */
    public TiendaCatalogoDTO catalogo() {
        List<TiendaCatalogoDTO.CategoriaItem> categorias = catalogService.listCategorias().stream()
                .map(c -> new TiendaCatalogoDTO.CategoriaItem(c.getIdCategoria(), c.getNombre()))
                .toList();

        List<TiendaCatalogoDTO.ProductoItem> productos = catalogService.listProductosDisponibles().stream()
                .map(p -> new TiendaCatalogoDTO.ProductoItem(
                        p.getIdProducto(), p.getCodigo(), p.getNombre(), p.getPrecio(),
                        p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null,
                        p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                        p.getImagenUrl()))
                .toList();

        List<TiendaCatalogoDTO.PromocionItem> promociones = catalogService.listPromociones().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActiva()))
                .map(p -> new TiendaCatalogoDTO.PromocionItem(p.getIdPromocion(), p.getDescripcion()))
                .toList();

        return new TiendaCatalogoDTO(categorias, productos, promociones);
    }

    /**
     * Validacion informativa de un codigo de cupon para el checkout: solo confirma
     * si existe y esta activo. El descuento real lo aplica {@code /api/tienda/cotizar}
     * cuando recibe el codigo, que es el total autoritativo.
     */
    public TiendaPromoValidacionResponseDTO validarPromo(String codigo) {
        return promocionRepository.findByCodigoAndActivaTrue(codigo)
                .map(Promocion::getDescripcion)
                .map(TiendaPromoValidacionResponseDTO::valido)
                .orElseGet(() -> TiendaPromoValidacionResponseDTO.invalido("Codigo no valido"));
    }

    /**
     * Preview de precios para la tienda: DELEGA en la misma cotizacion del POS
     * (total autoritativo con promociones y cupon) y solo adapta la forma de salida.
     * Un codigo invalido no es error: la cotizacion sale sin cupon aplicado.
     */
    public TiendaCotizacionDTO cotizar(List<DetallePedidoDTO> items, String codigo) {
        Promocion cupon = catalogService.getCuponActivo(codigo);
        return aFormatoTienda(pedidoService.cotizar(items, codigo), cupon);
    }

    /** Historial de pedidos de la cuenta autenticada, del mas reciente al mas antiguo. */
    public List<TiendaPedidoResumenDTO> misPedidos() {
        ClienteCuenta cuenta = cuentaService.cuentaActual();
        return pedidoWebRepository.findByCuentaIdOrderByFechaDesc(cuenta.getId()).stream()
                .map(this::aResumen)
                .toList();
    }

    private TiendaPedidoResumenDTO aResumen(PedidoWeb pedidoWeb) {
        Pedido pedido = pedidoWeb.getPedido();
        BigDecimal total = BigDecimal.ZERO;
        List<TiendaPedidoResumenDTO.Item> items = new ArrayList<>();
        // El total del pedido es la suma de sus lineas (precios con IGV incluido).
        for (DetallePedido detalle : pedidoService.getDetalle(pedido.getIdPedido())) {
            items.add(new TiendaPedidoResumenDTO.Item(detalle.getProducto().getNombre(), detalle.getCantidad()));
            total = total.add(detalle.getSubtotal());
        }
        return new TiendaPedidoResumenDTO(pedido.getIdPedido(), pedido.getFecha(), total,
                pedido.getEstado().name(), pedidoWeb.getModalidad().name(), items);
    }

    /**
     * Crea un pedido web: re-cotiza en servidor, valida entrega y pagos, resuelve
     * las tarjetas de la cuenta y delega la venta en el flujo del POS. Al final
     * persiste el detalle web (modalidad/direccion) en pedido_web.
     */
    @Transactional
    public TiendaPedidoCreadoDTO crearPedido(TiendaPedidoDTO dto) {
        ClienteCuenta cuenta = cuentaService.cuentaActual();

        ModalidadEntrega modalidad = parseModalidad(dto.getEntrega().getModalidad());
        String direccion = resolverDireccion(modalidad, dto.getEntrega());

        // Total autoritativo: se recalcula SIEMPRE en servidor (con el cupon si lo hay).
        CotizacionDTO cotizacion = pedidoService.cotizar(dto.getItems(), dto.getCodigo());
        validarPagos(dto.getPagos(), cotizacion.getTotal());

        List<PagoDTO> pagosPos = new ArrayList<>();
        for (TiendaPagoDTO pago : dto.getPagos()) {
            MetodoPago metodo = resolverMetodo(pago.getMetodo());
            if (METODO_TARJETA.equalsIgnoreCase(pago.getMetodo())) {
                procesarTarjeta(cuenta, pago.getTarjeta());
            }
            pagosPos.add(new PagoDTO(metodo.getIdMetodoPago(), pago.getMonto()));
        }

        Usuario usuarioTienda = usuarioRepository.findByUsername(USUARIO_TIENDA)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el usuario sistema '" + USUARIO_TIENDA + "' (ver bd/migracion_tienda.sql)"));

        // Mismo flujo del POS: descuenta stock, genera boleta con correlativo y avisa a cocina.
        PedidoDTO pedidoPos = new PedidoDTO();
        pedidoPos.setClienteNombre(cuenta.getCliente().getNombre());
        pedidoPos.setClienteTelefono(cuenta.getCliente().getTelefono());
        pedidoPos.setIdMetodoPago(pagosPos.get(0).getIdMetodoPago());
        pedidoPos.setItems(dto.getItems());
        pedidoPos.setPagos(pagosPos);
        pedidoPos.setCodigo(dto.getCodigo());
        BoletaDTO boleta = pedidoService.crearPedido(pedidoPos, usuarioTienda.getIdUsuario());

        Pedido pedido = pedidoService.getPedido(boleta.getIdPedido());

        PedidoWeb pedidoWeb = new PedidoWeb();
        pedidoWeb.setPedido(pedido);
        pedidoWeb.setCuenta(cuenta);
        pedidoWeb.setModalidad(modalidad);
        pedidoWeb.setDireccion(direccion);
        pedidoWeb.setFecha(LocalDateTime.now());
        pedidoWebRepository.save(pedidoWeb);

        return new TiendaPedidoCreadoDTO(pedido.getIdPedido(), boleta.getTotal(), pedido.getEstado().name());
    }

    // ---- Validaciones y resolucion de datos del canal web ----------

    private ModalidadEntrega parseModalidad(String valor) {
        try {
            return ModalidadEntrega.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Modalidad de entrega invalida: usa DELIVERY o RECOJO");
        }
    }

    /** DELIVERY exige direccion (max 150 via Bean Validation); RECOJO no la lleva. */
    private String resolverDireccion(ModalidadEntrega modalidad, TiendaEntregaDTO entrega) {
        if (modalidad != ModalidadEntrega.DELIVERY) {
            return null;
        }
        String direccion = entrega.getDireccion() != null ? entrega.getDireccion().trim() : "";
        if (direccion.isEmpty()) {
            throw new IllegalArgumentException("El delivery necesita una direccion de entrega");
        }
        return direccion;
    }

    private void validarPagos(List<TiendaPagoDTO> pagos, BigDecimal total) {
        BigDecimal pagado = pagos.stream()
                .map(TiendaPagoDTO::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (pagado.compareTo(total) != 0) {
            throw new PagosNoCuadranException(total, pagado);
        }
    }

    /** Resuelve el metodo por nombre (EFECTIVO, TARJETA, YAPE, PLIN, BILLETERA) contra metodo_pago. */
    private MetodoPago resolverMetodo(String metodo) {
        String buscado = metodo.trim();
        return metodoPagoRepository.findByActivoTrue().stream()
                .filter(m -> m.getDescripcion() != null && m.getDescripcion().equalsIgnoreCase(buscado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El metodo de pago '" + buscado + "' no esta disponible"));
    }

    /**
     * Tarjeta de un pago: si referencia una guardada, verifica que sea de la
     * cuenta; si es nueva y pide guardarla, persiste SOLO marca + ultimos 4 +
     * titular. El numero completo se descarta aqui y el CVV nunca llega al backend.
     */
    private void procesarTarjeta(ClienteCuenta cuenta, TiendaTarjetaDTO tarjeta) {
        if (tarjeta == null) {
            return;
        }
        if (tarjeta.getIdGuardada() != null) {
            tarjetaRepository.findById(tarjeta.getIdGuardada())
                    .filter(t -> t.getCuenta().getId().equals(cuenta.getId()))
                    .orElseThrow(() -> new TarjetaNoAutorizadaException(
                            "La tarjeta indicada no pertenece a tu cuenta"));
            return;
        }
        if (!Boolean.TRUE.equals(tarjeta.getGuardar())) {
            return;
        }
        String numero = tarjeta.getNumero() != null ? tarjeta.getNumero().replaceAll("\\D", "") : "";
        if (numero.length() < 13 || numero.length() > 16) {
            throw new IllegalArgumentException("El numero de tarjeta no es valido");
        }
        String titular = tarjeta.getTitular() != null ? tarjeta.getTitular().trim() : "";
        if (titular.isEmpty()) {
            throw new IllegalArgumentException("La tarjeta necesita el nombre del titular");
        }
        ClienteTarjeta nueva = new ClienteTarjeta();
        nueva.setCuenta(cuenta);
        nueva.setMarca(derivarMarca(numero));
        nueva.setUltimos4(numero.substring(numero.length() - 4));
        nueva.setTitular(titular);
        nueva.setFechaRegistro(LocalDateTime.now());
        tarjetaRepository.save(nueva);
    }

    /** Marca por prefijo del PAN: 4 -> Visa; 51-55 o 22-27 -> Mastercard; otro -> Tarjeta. */
    private String derivarMarca(String numero) {
        if (numero.startsWith("4")) {
            return "Visa";
        }
        if (numero.matches("^(5[1-5]|2[2-7]).*")) {
            return "Mastercard";
        }
        return "Tarjeta";
    }

    private TiendaCotizacionDTO aFormatoTienda(CotizacionDTO cotizacion, Promocion cupon) {
        List<TiendaCotizacionDTO.Linea> lineas = cotizacion.getLineas().stream()
                .map(l -> new TiendaCotizacionDTO.Linea(
                        l.getIdProducto(),
                        catalogService.getProducto(l.getIdProducto()).getNombre(),
                        l.getCantidad(),
                        l.getPrecioUnitario(),
                        l.getDescuento(),
                        l.getSubtotalLinea()))
                .toList();
        return new TiendaCotizacionDTO(lineas, cotizacion.getTotal(),
                cupon != null, cupon != null ? cupon.getDescripcion() : null);
    }
}
