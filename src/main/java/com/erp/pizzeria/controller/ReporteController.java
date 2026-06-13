package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.ProveedorReporteDTO;
import com.erp.pizzeria.dto.TipoMovReporteDTO;
import com.erp.pizzeria.dto.TopProductoDTO;
import com.erp.pizzeria.model.Compra;
import com.erp.pizzeria.model.Movimiento;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.service.CompraService;
import com.erp.pizzeria.service.InventarioService;
import com.erp.pizzeria.service.PedidoService;
import com.erp.pizzeria.service.ReporteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class ReporteController {

    private final ReporteService reporteService;
    private final CompraService compraService;
    private final InventarioService inventarioService;
    private final PedidoService pedidoService;

    public ReporteController(ReporteService reporteService,
                             CompraService compraService,
                             InventarioService inventarioService,
                             PedidoService pedidoService) {
        this.reporteService = reporteService;
        this.compraService = compraService;
        this.inventarioService = inventarioService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/reportes")
    public String reportes(Model model) {
        List<TopProductoDTO> top = reporteService.getTopProductos();
        long topMax = top.stream().mapToLong(TopProductoDTO::getCantidad).max().orElse(1);

        List<Pedido> anulados = pedidoService.listPedidos().stream()
                .filter(p -> p.getEstado() == EstadoPedido.ANULADO)
                .toList();

        model.addAttribute("active", "reportes");
        model.addAttribute("pageTitle", "Reportes");
        model.addAttribute("salesStats", reporteService.getSalesStats());
        model.addAttribute("topProductos", top);
        model.addAttribute("topMax", topMax);
        model.addAttribute("comprasPorProveedor", comprasPorProveedor());
        model.addAttribute("movimientosPorTipo", movimientosPorTipo());
        model.addAttribute("anulados", anulados);
        model.addAttribute("boletas", pedidoService.getBoletasPorPedido());
        return "admin/reportes";
    }

    private List<ProveedorReporteDTO> comprasPorProveedor() {
        Map<String, ProveedorReporteDTO> mapa = new LinkedHashMap<>();
        for (Compra c : compraService.listCompras()) {
            String nombre = c.getProveedor().getNombre();
            ProveedorReporteDTO dto = mapa.computeIfAbsent(nombre,
                    n -> new ProveedorReporteDTO(n, 0, BigDecimal.ZERO, "-"));
            dto.setCompras(dto.getCompras() + 1);
            dto.setTotal(dto.getTotal().add(c.getTotal()));
            String fecha = c.getFecha().toString();
            if ("-".equals(dto.getUltimaCompra()) || fecha.compareTo(dto.getUltimaCompra()) > 0) {
                dto.setUltimaCompra(fecha);
            }
        }
        return new ArrayList<>(mapa.values());
    }

    private List<TipoMovReporteDTO> movimientosPorTipo() {
        Map<String, TipoMovReporteDTO> mapa = new LinkedHashMap<>();
        for (Movimiento m : inventarioService.listMovimientos()) {
            String desc = m.getTipoMovimiento().getDescripcion();
            TipoMovReporteDTO dto = mapa.computeIfAbsent(desc,
                    d -> new TipoMovReporteDTO(d, m.getTipoMovimiento().getOperacion(), 0));
            dto.setRegistros(dto.getRegistros() + 1);
        }
        return mapa.values().stream()
                .sorted(Comparator.comparing(TipoMovReporteDTO::getTipo))
                .toList();
    }
}
