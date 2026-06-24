package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.BoletaDTO;
import com.erp.pizzeria.dto.CajeroOpcion;
import com.erp.pizzeria.dto.DetallePedidoDTO;
import com.erp.pizzeria.dto.PagoDTO;
import com.erp.pizzeria.dto.PedidoCocinaDTO;
import com.erp.pizzeria.dto.PedidoDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Boleta;
import com.erp.pizzeria.model.Cliente;
import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.MetodoPago;
import com.erp.pizzeria.model.Pago;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.repository.BoletaRepository;
import com.erp.pizzeria.repository.ClienteRepository;
import com.erp.pizzeria.repository.DetallePedidoRepository;
import com.erp.pizzeria.repository.MetodoPagoRepository;
import com.erp.pizzeria.repository.PagoRepository;
import com.erp.pizzeria.repository.PedidoRepository;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.audit.Audit;
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

    public PedidoService(PedidoRepository pedidoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         BoletaRepository boletaRepository,
                         PagoRepository pagoRepository,
                         ClienteRepository clienteRepository,
                         MetodoPagoRepository metodoPagoRepository,
                         UsuarioRepository usuarioRepository,
                         CatalogService catalogService,
                         InventarioService inventarioService) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.boletaRepository = boletaRepository;
        this.pagoRepository = pagoRepository;
        this.clienteRepository = clienteRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogService = catalogService;
        this.inventarioService = inventarioService;
    }

    // ---- Lecturas --------------------------------------------------

    public List<Pedido> listPedidos() {
        return pedidoRepository.findAll();
    }

    public Page<Pedido> buscarPedidos(EstadoPedido estado, String cliente, Integer cajero,
                                      LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                      BigDecimal totalMin, BigDecimal totalMax, Pageable pageable) {
        return pedidoRepository.buscar(estado, cliente, cajero, fechaDesde, fechaHasta, totalMin, totalMax, pageable);
    }

    public List<CajeroOpcion> listarCajeros() {
        return pedidoRepository.findCajeros().stream()
                .map(u -> new CajeroOpcion(u.getIdUsuario(), nombreCajero(u)))
                .toList();
    }

    private String nombreCajero(Usuario u) {
        return u.getEmpleado() != null
                ? u.getEmpleado().getNombre() + " " + u.getEmpleado().getApellido()
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

        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetallePedidoDTO item : dto.getItems()) {
            Producto producto = productos.get(item.getIdProducto());
            BigDecimal descuento = catalogService.calcularDescuento(producto, item.getCantidad());
            BigDecimal lineaSubtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(item.getCantidad()))
                    .subtract(descuento)
                    .setScale(2, RoundingMode.HALF_UP);

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setDescuento(descuento);
            detalle.setSubtotal(lineaSubtotal);
            detalle.setObservacion(item.getObservacion());
            detallePedidoRepository.save(detalle);

            subtotal = subtotal.add(lineaSubtotal);
        }

        BigDecimal igv = subtotal.multiply(IGV).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv);

        List<PagoDTO> pagos = resolverPagos(dto, total);
        Map<Integer, MetodoPago> metodos = resolverMetodos(pagos);

        Boleta boleta = new Boleta();
        boleta.setSubtotal(subtotal);
        boleta.setIgv(igv);
        boleta.setTotal(total);
        boleta.setMetodoPago(metodos.get(pagos.get(0).getIdMetodoPago()));
        boleta.setPedido(pedido);
        boleta = boletaRepository.save(boleta);

        for (PagoDTO parte : pagos) {
            Pago pago = new Pago();
            pago.setPedido(pedido);
            pago.setMetodoPago(metodos.get(parte.getIdMetodoPago()));
            pago.setMonto(parte.getMonto().setScale(2, RoundingMode.HALF_UP));
            pagoRepository.save(pago);
        }

        String documento = String.format("P-%04d", pedido.getIdPedido());
        inventarioService.aplicarMovimiento("Venta", documento, "Consumo por venta", usuario, null, consumo);

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
        Pedido pedido = getPedido(idPedido);
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido #" + idPedido + " esta anulado y no admite cambios de estado");
        }
        pedido.setEstado(estado);
        return pedidoRepository.save(pedido);
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
        return pedido;
    }
}
