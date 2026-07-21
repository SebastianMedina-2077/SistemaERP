package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.AdicionalSeleccionadoDTO;
import com.erp.pizzeria.dto.BoletaDTO;
import com.erp.pizzeria.dto.CajeroOpcion;
import com.erp.pizzeria.dto.CotizacionDTO;
import com.erp.pizzeria.dto.DetallePedidoDTO;
import com.erp.pizzeria.dto.PagoDTO;
import com.erp.pizzeria.dto.PedidoCocinaDTO;
import com.erp.pizzeria.dto.PedidoDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Adicional;
import com.erp.pizzeria.model.Boleta;
import com.erp.pizzeria.model.Cliente;
import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.DetallePedidoAdicional;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.MetodoPago;
import com.erp.pizzeria.model.Pago;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.model.Promocion;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.model.enums.TipoComprobante;
import com.erp.pizzeria.repository.AdicionalRepository;
import com.erp.pizzeria.repository.BoletaRepository;
import com.erp.pizzeria.repository.ClienteRepository;
import com.erp.pizzeria.repository.DetallePedidoAdicionalRepository;
import com.erp.pizzeria.repository.DetallePedidoRepository;
import com.erp.pizzeria.repository.MetodoPagoRepository;
import com.erp.pizzeria.repository.ProductoAdicionalRepository;
import com.erp.pizzeria.repository.PagoRepository;
import com.erp.pizzeria.repository.PedidoRepository;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.audit.Audit;
import com.erp.pizzeria.event.PedidoEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PedidoService {

    private static final BigDecimal IGV = new BigDecimal("0.18");
    // La cocina solo ve pedidos por preparar; al marcarlos ATENDIDO salen de la cola.
    private static final List<EstadoPedido> ESTADOS_COCINA =
            List.of(EstadoPedido.PENDIENTE, EstadoPedido.PREPARANDO);

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final BoletaRepository boletaRepository;
    private final PagoRepository pagoRepository;
    private final ClienteRepository clienteRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogService catalogService;
    private final InventarioService inventarioService;
    private final CorrelativoService correlativoService;
    private final ClienteEmpresaService clienteEmpresaService;
    private final EmailComprobanteService emailComprobanteService;
    private final ApplicationEventPublisher eventPublisher;
    private final AdicionalRepository adicionalRepository;
    private final ProductoAdicionalRepository productoAdicionalRepository;
    private final DetallePedidoAdicionalRepository detallePedidoAdicionalRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         BoletaRepository boletaRepository,
                         PagoRepository pagoRepository,
                         ClienteRepository clienteRepository,
                         MetodoPagoRepository metodoPagoRepository,
                         UsuarioRepository usuarioRepository,
                         CatalogService catalogService,
                         InventarioService inventarioService,
                         CorrelativoService correlativoService,
                         ClienteEmpresaService clienteEmpresaService,
                         EmailComprobanteService emailComprobanteService,
                         ApplicationEventPublisher eventPublisher,
                         AdicionalRepository adicionalRepository,
                         ProductoAdicionalRepository productoAdicionalRepository,
                         DetallePedidoAdicionalRepository detallePedidoAdicionalRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.boletaRepository = boletaRepository;
        this.pagoRepository = pagoRepository;
        this.clienteRepository = clienteRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogService = catalogService;
        this.inventarioService = inventarioService;
        this.correlativoService = correlativoService;
        this.clienteEmpresaService = clienteEmpresaService;
        this.emailComprobanteService = emailComprobanteService;
        this.eventPublisher = eventPublisher;
        this.adicionalRepository = adicionalRepository;
        this.productoAdicionalRepository = productoAdicionalRepository;
        this.detallePedidoAdicionalRepository = detallePedidoAdicionalRepository;
    }

    // ---- Lecturas --------------------------------------------------

    public List<Pedido> listPedidos() {
        return pedidoRepository.findAll();
    }

    public Page<Pedido> buscarPedidos(Integer numero, EstadoPedido estado, String cliente, Integer cajero,
                                      LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                      BigDecimal totalMin, BigDecimal totalMax, Pageable pageable) {
        return pedidoRepository.buscar(numero, estado, cliente, cajero, fechaDesde, fechaHasta, totalMin, totalMax, pageable);
    }

    public List<CajeroOpcion> listarCajeros() {
        return pedidoRepository.findCajeros().stream()
                .map(u -> new CajeroOpcion(u.getIdUsuario(), nombreCajero(u)))
                .toList();
    }

    private String nombreCajero(Usuario u) {
        return u.getEmpleado() != null
                ? u.getEmpleado().getNombre() + " " + u.getEmpleado().getApellidoPaterno() + " " + u.getEmpleado().getApellidoMaterno()
                : u.getUsername();
    }

    /** Mapa idPedido -> Boleta solo para los pedidos dados (evita cargar todas las boletas). */
    public Map<Integer, Boleta> getBoletasDe(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = pedidos.stream().map(Pedido::getIdPedido).toList();
        Map<Integer, Boleta> mapa = new LinkedHashMap<>();
        for (Boleta b : boletaRepository.findByPedido_IdPedidoIn(ids)) {
            if (b.getPedido() != null) {
                mapa.put(b.getPedido().getIdPedido(), b);
            }
        }
        return mapa;
    }

    public Pedido getPedido(Integer idPedido) {
        return pedidoRepository.findById(idPedido)
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", idPedido));
    }

    public List<DetallePedido> getDetalle(Integer idPedido) {
        return detallePedidoRepository.findByPedido_IdPedido(idPedido);
    }

    /** Desglose de pago de un pedido: una parte si fue pago simple, varias si fue mixto. */
    public List<Pago> getPagos(Integer idPedido) {
        return pagoRepository.findByPedido_IdPedidoOrderByIdPago(idPedido);
    }

    public List<MetodoPago> listMetodosPago() {
        return metodoPagoRepository.findByActivoTrue();
    }

    public Map<Integer, Boleta> getBoletasPorPedido() {
        Map<Integer, Boleta> mapa = new LinkedHashMap<>();
        for (Boleta b : boletaRepository.findAll()) {
            if (b.getPedido() != null) {
                mapa.put(b.getPedido().getIdPedido(), b);
            }
        }
        return mapa;
    }

    public List<Pedido> getKitchenOrders() {
        return pedidoRepository.findByEstadoInOrderByFechaAsc(ESTADOS_COCINA);
    }

    public List<PedidoCocinaDTO> getKitchenOrdersDTO() {
        return getKitchenOrders().stream()
                .map(p -> PedidoCocinaDTO.from(p, getDetalle(p.getIdPedido())))
                .toList();
    }

    /**
     * Preview de precios: total AUTORITATIVO del pedido (descuentos de promocion
     * e IGV incluido) usando el MISMO calculo por linea que {@code crearPedido}.
     * No verifica stock ni persiste nada.
     */
    @Transactional(readOnly = true)
    public CotizacionDTO cotizar(List<DetallePedidoDTO> items) {
        return cotizar(items, null);
    }

    /** Cotizacion con cupon opcional: un codigo invalido o inactivo simplemente se ignora. */
    @Transactional(readOnly = true)
    public CotizacionDTO cotizar(List<DetallePedidoDTO> items, String codigoCupon) {
        Promocion cupon = catalogService.getCuponActivo(codigoCupon);
        List<CotizacionDTO.Linea> lineas = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedidoDTO item : items) {
            Producto producto = catalogService.getProducto(item.getIdProducto());
            CotizacionDTO.Linea linea = calcularLinea(producto, item, cupon);
            lineas.add(linea);
            total = total.add(linea.getSubtotalLinea());
        }

        BigDecimal igv = calcularIgvIncluido(total);
        BigDecimal subtotal = total.subtract(igv);

        return CotizacionDTO.builder()
                .subtotal(subtotal)
                .igv(igv)
                .total(total)
                .lineas(lineas)
                .build();
    }

    /**
     * Calculo de una linea (unico punto de verdad, reutilizado por crearPedido y cotizar):
     * subtotalLinea = precio x cantidad - descuento, redondeado a 2 (HALF_UP).
     */
    private CotizacionDTO.Linea calcularLinea(Producto producto, DetallePedidoDTO item, Promocion cupon) {
        int cantidad = item.getCantidad();
        BigDecimal precioUnitario = producto.getPrecio();
        BigDecimal descuento = catalogService.calcularDescuento(producto, cantidad, cupon);
        List<CotizacionDTO.LineaAdicional> adicionales = resolverAdicionales(producto, item.getAdicionales());
        // Los adicionales suman por encima del producto y aplican a cada unidad de la linea.
        BigDecimal adicionalPorUnidad = adicionales.stream()
                .map(a -> a.getPrecioUnitario().multiply(BigDecimal.valueOf(a.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotalLinea = precioUnitario
                .multiply(BigDecimal.valueOf(cantidad))
                .subtract(descuento)
                .add(adicionalPorUnidad.multiply(BigDecimal.valueOf(cantidad)))
                .setScale(2, RoundingMode.HALF_UP);
        return CotizacionDTO.Linea.builder()
                .idProducto(producto.getIdProducto())
                .cantidad(cantidad)
                .precioUnitario(precioUnitario)
                .descuento(descuento)
                .subtotalLinea(subtotalLinea)
                .adicionales(adicionales)
                .build();
    }

    /** Resuelve en servidor los adicionales elegidos: valida vinculo con el producto y disponibilidad. */
    private List<CotizacionDTO.LineaAdicional> resolverAdicionales(Producto producto, List<AdicionalSeleccionadoDTO> seleccion) {
        if (seleccion == null || seleccion.isEmpty()) return List.of();
        Set<Integer> permitidos = productoAdicionalRepository.findByProducto_IdProducto(producto.getIdProducto())
                .stream().map(pa -> pa.getAdicional().getIdAdicional()).collect(Collectors.toSet());
        List<CotizacionDTO.LineaAdicional> resultado = new java.util.ArrayList<>();
        for (AdicionalSeleccionadoDTO sel : seleccion) {
            Adicional adicional = adicionalRepository.findById(sel.getIdAdicional())
                    .orElseThrow(() -> new ResourceNotFoundException("Adicional no encontrado: " + sel.getIdAdicional()));
            if (!permitidos.contains(adicional.getIdAdicional()) || !Boolean.TRUE.equals(adicional.getDisponible())) {
                throw new IllegalArgumentException("El adicional '" + adicional.getNombre() + "' no esta disponible para este producto");
            }
            resultado.add(new CotizacionDTO.LineaAdicional(adicional.getIdAdicional(), adicional.getNombre(),
                    adicional.getPrecio(), sel.getCantidad()));
        }
        return resultado;
    }

    /** Persiste los adicionales elegidos en una linea, congelando el precio de venta. */
    private void guardarAdicionales(DetallePedido detalle, List<CotizacionDTO.LineaAdicional> adicionales) {
        if (adicionales == null) return;
        for (CotizacionDTO.LineaAdicional adic : adicionales) {
            DetallePedidoAdicional dpa = new DetallePedidoAdicional();
            dpa.setDetallePedido(detalle);
            dpa.setAdicional(adicionalRepository.getReferenceById(adic.getIdAdicional()));
            dpa.setCantidad(adic.getCantidad());
            dpa.setPrecioUnitario(adic.getPrecioUnitario());
            detallePedidoAdicionalRepository.save(dpa);
        }
    }

    /** Extrae el IGV incluido en un total: igv = total x 18/118, redondeado a 2 (HALF_UP). */
    private BigDecimal calcularIgvIncluido(BigDecimal total) {
        return total.multiply(IGV)
                .divide(BigDecimal.ONE.add(IGV), 2, RoundingMode.HALF_UP);
    }

    // ---- Operaciones transaccionales -------------------------------

    @Audit(accion = "CREAR", entidad = "Pedido")
    @Transactional
    public BoletaDTO crearPedido(PedidoDTO dto, Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", idUsuario));

        Map<Integer, Producto> productos = new LinkedHashMap<>();
        Map<Insumo, BigDecimal> consumo = new LinkedHashMap<>();
        for (DetallePedidoDTO item : dto.getItems()) {
            Producto producto = productos.computeIfAbsent(item.getIdProducto(), catalogService::getProducto);
            inventarioService.combinar(consumo, inventarioService.consumoDeProducto(producto, item.getCantidad()));
        }
        inventarioService.verificarDisponibilidad(consumo);

        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getClienteNombre());
        cliente.setTelefono(dto.getClienteTelefono());
        cliente = clienteRepository.save(cliente);

        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setUsuario(usuario);
        pedido.setCliente(cliente);
        pedido = pedidoRepository.save(pedido);

        // Mismo cupon que la cotizacion previa: el total persistido cuadra con el cotizado.
        Promocion cupon = catalogService.getCuponActivo(dto.getCodigo());

        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detalles = new java.util.ArrayList<>();
        for (DetallePedidoDTO item : dto.getItems()) {
            Producto producto = productos.get(item.getIdProducto());
            // MISMO calculo por linea que usa la cotizacion (preview de precios).
            CotizacionDTO.Linea linea = calcularLinea(producto, item, cupon);

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(linea.getPrecioUnitario());
            detalle.setDescuento(linea.getDescuento());
            detalle.setSubtotal(linea.getSubtotalLinea());
            detalle.setObservacion(item.getObservacion());
            detallePedidoRepository.save(detalle);
            detalles.add(detalle);
            guardarAdicionales(detalle, linea.getAdicionales());

            total = total.add(linea.getSubtotalLinea());
        }

        // Los precios ya incluyen IGV: el total es la suma directa y el IGV se extrae
        // (igv = total x 18/118); el subtotal queda como base imponible de la boleta.
        BigDecimal igv = calcularIgvIncluido(total);
        BigDecimal subtotal = total.subtract(igv);

        List<PagoDTO> pagos = resolverPagos(dto, total);
        Map<Integer, MetodoPago> metodos = resolverMetodos(pagos);

        // Comprobante: serie y correlativo SECUENCIAL SIN HUECOS (numeracion de negocio).
        TipoComprobante tipo = parseTipoComprobante(dto.getTipoComprobante());
        int correlativo = correlativoService.siguiente(tipo.getSerie());

        Boleta boleta = new Boleta();
        boleta.setSubtotal(subtotal);
        boleta.setIgv(igv);
        boleta.setTotal(total);
        boleta.setTipoComprobante(tipo);
        boleta.setSerie(tipo.getSerie());
        boleta.setCorrelativo(correlativo);
        boleta.setMesa(normalizarMesa(dto.getMesa()));
        aplicarAdquiriente(boleta, tipo, dto);
        boleta.setMetodoPago(metodos.get(pagos.get(0).getIdMetodoPago()));
        boleta.setPedido(pedido);
        boleta = boletaRepository.save(boleta);

        // Boleta electronica: se envia por email. El envio nunca rompe la venta.
        if (tipo.requiereEnvioEmail()) {
            boleta.setEmailEstado(emailComprobanteService.enviar(boleta, detalles, boleta.getClienteEmail()));
        } else {
            boleta.setEmailEstado("NO_APLICA");
        }

        for (PagoDTO parte : pagos) {
            Pago pago = new Pago();
            pago.setPedido(pedido);
            pago.setMetodoPago(metodos.get(parte.getIdMetodoPago()));
            pago.setMonto(parte.getMonto().setScale(2, RoundingMode.HALF_UP));
            pagoRepository.save(pago);
        }

        String documento = String.format("P-%04d", pedido.getIdPedido());
        inventarioService.aplicarMovimiento("Venta", documento, "Consumo por venta", usuario, null, consumo);

        // Aviso en tiempo real a las pantallas (se entrega tras el commit).
        eventPublisher.publishEvent(new PedidoEvent("pedido-nuevo",
                Map.of("idPedido", pedido.getIdPedido(), "cliente", cliente.getNombre())));

        return BoletaDTO.from(boleta);
    }

    /** Usa el desglose de pagos del DTO o, si no viene, un unico pago por el total. Valida que sumen el total. */
    private List<PagoDTO> resolverPagos(PedidoDTO dto, BigDecimal total) {
        List<PagoDTO> pagos = (dto.getPagos() != null && !dto.getPagos().isEmpty())
                ? dto.getPagos()
                : List.of(new PagoDTO(dto.getIdMetodoPago(), total));

        BigDecimal suma = pagos.stream()
                .map(PagoDTO::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (suma.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "La suma de los pagos (S/ " + suma + ") no coincide con el total (S/ " + total + ")");
        }
        return pagos;
    }

    /** Convierte el tipo de comprobante del DTO; por defecto BOLETA. */
    private TipoComprobante parseTipoComprobante(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoComprobante.BOLETA;
        }
        try {
            return TipoComprobante.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tipo de comprobante invalido: " + valor);
        }
    }

    /** Normaliza la mesa: vacio/null -> null (para llevar / sin mesa). */
    private String normalizarMesa(String mesa) {
        return (mesa == null || mesa.isBlank()) ? null : mesa.trim();
    }

    /** Completa los datos del adquiriente segun el tipo de comprobante y valida lo obligatorio. */
    private void aplicarAdquiriente(Boleta boleta, TipoComprobante tipo, PedidoDTO dto) {
        switch (tipo) {
            case FACTURA -> {
                String ruc = dto.getClienteRuc() != null ? dto.getClienteRuc().trim() : "";
                String razon = dto.getClienteRazonSocial() != null ? dto.getClienteRazonSocial().trim() : "";
                if (!ruc.matches("\\d{11}")) {
                    throw new IllegalArgumentException("La factura requiere un RUC valido de 11 digitos");
                }
                if (razon.isBlank()) {
                    throw new IllegalArgumentException("La factura requiere la razon social del cliente");
                }
                boleta.setClienteDocumento(ruc);
                boleta.setClienteRazonSocial(razon);
                // Registra el RUC para autocompletar en futuras ventas.
                clienteEmpresaService.registrar(ruc, razon);
            }
            case BOLETA_ELECTRONICA -> {
                String email = dto.getClienteEmail() != null ? dto.getClienteEmail().trim() : "";
                if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    throw new IllegalArgumentException("La boleta electronica requiere un email valido");
                }
                boleta.setClienteEmail(email);
                boleta.setClienteDocumento(limpiarDni(dto.getClienteDni()));
            }
            default -> // BOLETA: DNI opcional
                    boleta.setClienteDocumento(limpiarDni(dto.getClienteDni()));
        }
    }

    /** DNI opcional: null si vacio; valida 8 digitos si viene. */
    private String limpiarDni(String dni) {
        if (dni == null || dni.isBlank()) {
            return null;
        }
        String limpio = dni.trim();
        if (!limpio.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe tener 8 digitos");
        }
        return limpio;
    }

    /** Resuelve cada metodo de pago una sola vez. */
    private Map<Integer, MetodoPago> resolverMetodos(List<PagoDTO> pagos) {
        Map<Integer, MetodoPago> metodos = new LinkedHashMap<>();
        for (PagoDTO parte : pagos) {
            metodos.computeIfAbsent(parte.getIdMetodoPago(), id ->
                    metodoPagoRepository.findById(id)
                            .orElseThrow(() -> ResourceNotFoundException.of("MetodoPago", id)));
        }
        return metodos;
    }

    @Audit(accion = "ESTADO", entidad = "Pedido")
    @Transactional
    public Pedido actualizarEstado(Integer idPedido, EstadoPedido estado) {
        // Anular tiene su propia operacion (exige motivo y revierte el stock);
        // no se permite llegar a ANULADO por un cambio de estado generico.
        if (estado == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("Para anular el pedido usa la opcion Anular (requiere motivo y revierte stock)");
        }
        Pedido pedido = getPedido(idPedido);
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido #" + idPedido + " esta anulado y no admite cambios de estado");
        }
        pedido.setEstado(estado);
        pedido = pedidoRepository.save(pedido);

        eventPublisher.publishEvent(new PedidoEvent("pedido-estado",
                Map.of("idPedido", pedido.getIdPedido(), "estado", pedido.getEstado().name())));

        return pedido;
    }

    @Audit(accion = "ANULAR", entidad = "Pedido")
    @Transactional
    public Pedido anularPedido(Integer idPedido, String motivo) {
        Pedido pedido = getPedido(idPedido);
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido #" + idPedido + " ya esta anulado");
        }

        Map<Insumo, BigDecimal> consumo = new LinkedHashMap<>();
        for (DetallePedido detalle : getDetalle(idPedido)) {
            inventarioService.combinar(consumo, inventarioService.consumoDeProducto(detalle.getProducto(), detalle.getCantidad()));
        }

        pedido.setEstado(EstadoPedido.ANULADO);
        pedido.setMotivoAnulacion(motivo);
        pedido = pedidoRepository.save(pedido);

        if (!consumo.isEmpty()) {
            String documento = String.format("A-%04d", pedido.getIdPedido());
            inventarioService.aplicarMovimiento("Ajuste", documento, "Reversion por anulacion", pedido.getUsuario(), null, consumo);
        }

        // Saca el pedido de la cola de cocina en el momento, no en el siguiente refresco.
        eventPublisher.publishEvent(new PedidoEvent("pedido-estado",
                Map.of("idPedido", pedido.getIdPedido(), "estado", pedido.getEstado().name())));

        return pedido;
    }
}
