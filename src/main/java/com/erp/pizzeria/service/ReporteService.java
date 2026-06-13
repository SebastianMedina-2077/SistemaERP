package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.StatDTO;
import com.erp.pizzeria.dto.TopProductoDTO;
import com.erp.pizzeria.model.Boleta;
import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.repository.BoletaRepository;
import com.erp.pizzeria.repository.DetallePedidoRepository;
import com.erp.pizzeria.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReporteService {

    private final PedidoRepository pedidoRepository;
    private final BoletaRepository boletaRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final InventarioService inventarioService;

    public ReporteService(PedidoRepository pedidoRepository,
                          BoletaRepository boletaRepository,
                          DetallePedidoRepository detallePedidoRepository,
                          InventarioService inventarioService) {
        this.pedidoRepository = pedidoRepository;
        this.boletaRepository = boletaRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.inventarioService = inventarioService;
    }

    private static String money(BigDecimal value) {
        BigDecimal v = value != null ? value : BigDecimal.ZERO;
        return "S/ " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean noAnulado(Boleta b) {
        return b.getPedido() != null && b.getPedido().getEstado() != EstadoPedido.ANULADO;
    }

    public List<StatDTO> getDashboardStats() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        BigDecimal ventas = boletaRepository.findAll().stream()
                .filter(this::noAnulado)
                .map(Boleta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendientes = pedidos.stream().filter(p -> p.getEstado() == EstadoPedido.PENDIENTE).count();
        long anulados = pedidos.stream().filter(p -> p.getEstado() == EstadoPedido.ANULADO).count();

        return List.of(
                new StatDTO("Ventas del dia", money(ventas)),
                new StatDTO("Pedidos pendientes", String.valueOf(pendientes)),
                new StatDTO("Insumos bajo stock", String.valueOf(inventarioService.getInsumosBajoStock().size())),
                new StatDTO("Pedidos anulados", String.valueOf(anulados))
        );
    }

    public List<StatDTO> getSalesStats() {
        List<Boleta> activas = boletaRepository.findAll().stream().filter(this::noAnulado).toList();
        BigDecimal total = activas.stream().map(Boleta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int validos = activas.size();
        BigDecimal promedio = validos > 0
                ? total.divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        long anulados = pedidoRepository.findByEstado(EstadoPedido.ANULADO).size();

        return List.of(
                new StatDTO("Ingresos", money(total)),
                new StatDTO("Pedidos validos", String.valueOf(validos)),
                new StatDTO("Ticket promedio", money(promedio)),
                new StatDTO("Anulados", String.valueOf(anulados))
        );
    }

    public List<TopProductoDTO> getTopProductos() {
        Map<Integer, TopProductoDTO> acumulado = new LinkedHashMap<>();
        for (DetallePedido d : detallePedidoRepository.findAll()) {
            acumulado.compute(d.getProducto().getIdProducto(), (id, dto) -> {
                if (dto == null) {
                    return new TopProductoDTO(id, d.getProducto().getNombre(), d.getCantidad());
                }
                dto.setCantidad(dto.getCantidad() + d.getCantidad());
                return dto;
            });
        }
        return acumulado.values().stream()
                .sorted(Comparator.comparingLong(TopProductoDTO::getCantidad).reversed())
                .toList();
    }
}
