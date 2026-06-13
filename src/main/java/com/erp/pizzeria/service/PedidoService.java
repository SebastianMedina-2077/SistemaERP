package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.BoletaDTO;
import com.erp.pizzeria.dto.DetallePedidoDTO;
import com.erp.pizzeria.dto.PedidoCocinaDTO;
import com.erp.pizzeria.dto.PedidoDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Boleta;
import com.erp.pizzeria.model.Cliente;
import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.MetodoPago;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.repository.BoletaRepository;
import com.erp.pizzeria.repository.ClienteRepository;
import com.erp.pizzeria.repository.DetallePedidoRepository;
import com.erp.pizzeria.repository.MetodoPagoRepository;
import com.erp.pizzeria.repository.PedidoRepository;
import com.erp.pizzeria.repository.UsuarioRepository;
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
    private static final List<EstadoPedido> ESTADOS_COCINA =
            List.of(EstadoPedido.PENDIENTE, EstadoPedido.PREPARANDO, EstadoPedido.ATENDIDO);

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final BoletaRepository boletaRepository;
    private final ClienteRepository clienteRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogService catalogService;
    private final InventarioService inventarioService;

    public PedidoService(PedidoRepository pedidoRepository,
                         DetallePedidoRepository detallePedidoRepository,
                         BoletaRepository boletaRepository,
                         ClienteRepository clienteRepository,
                         MetodoPagoRepository metodoPagoRepository,
                         UsuarioRepository usuarioRepository,
                         CatalogService catalogService,
                         InventarioService inventarioService) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.boletaRepository = boletaRepository;
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

    @Transactional
    public BoletaDTO crearPedido(PedidoDTO dto, Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", idUsuario));
        MetodoPago metodoPago = metodoPagoRepository.findById(dto.getIdMetodoPago())
                .orElseThrow(() -> ResourceNotFoundException.of("MetodoPago", dto.getIdMetodoPago()));

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
        Boleta boleta = new Boleta();
        boleta.setSubtotal(subtotal);
        boleta.setIgv(igv);
        boleta.setTotal(subtotal.add(igv));
        boleta.setMetodoPago(metodoPago);
        boleta.setPedido(pedido);
        boleta = boletaRepository.save(boleta);

        String documento = String.format("P-%04d", pedido.getIdPedido());
        inventarioService.aplicarMovimiento("Venta", documento, "Consumo por venta", usuario, null, consumo);

        return BoletaDTO.from(boleta);
    }

    @Transactional
    public Pedido actualizarEstado(Integer idPedido, EstadoPedido estado) {
        Pedido pedido = getPedido(idPedido);
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido #" + idPedido + " esta anulado y no admite cambios de estado");
        }
        pedido.setEstado(estado);
        return pedidoRepository.save(pedido);
    }

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
