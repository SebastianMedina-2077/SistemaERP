package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.TopProductoDTO;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.service.CompraService;
import com.erp.pizzeria.service.InventarioService;
import com.erp.pizzeria.service.PedidoService;
import com.erp.pizzeria.service.ReporteService;
import com.erp.pizzeria.util.PageQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReporteService reporteService;
    private final InventarioService inventarioService;
    private final CompraService compraService;
    private final PedidoService pedidoService;

    public AdminController(ReporteService reporteService,
                           InventarioService inventarioService,
                           CompraService compraService,
                           PedidoService pedidoService) {
        this.reporteService = reporteService;
        this.inventarioService = inventarioService;
        this.compraService = compraService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<TopProductoDTO> top = reporteService.getTopProductos();
        long topMax = top.stream().mapToLong(TopProductoDTO::getCantidad).max().orElse(1);

        model.addAttribute("active", "dashboard");
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("stats", reporteService.getDashboardStats());
        model.addAttribute("topProductos", top);
        model.addAttribute("topMax", topMax);
        model.addAttribute("lowStock", inventarioService.getInsumosBajoStock());
        model.addAttribute("compras", compraService.listCompras());
        model.addAttribute("movimientos", inventarioService.listMovimientos());
        return "admin/dashboard";
    }

    private static final Map<String, String> RANGOS_TOTAL = crearRangosTotal();

    @GetMapping("/pedidos")
    public String pedidos(@RequestParam(required = false) String cliente,
                          @RequestParam(required = false) Integer cajero,
                          @RequestParam(required = false) String estado,
                          @RequestParam(required = false) String total,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          @PageableDefault(size = 10, sort = "idPedido", direction = Sort.Direction.DESC) Pageable pageable,
                          Model model) {
        EstadoPedido estadoFiltro = parseEstadoFiltro(estado);
        String clienteFiltro = normalizar(cliente);
        String totalFiltro = RANGOS_TOTAL.containsKey(total) ? total : null;
        BigDecimal[] rango = parseRangoTotal(totalFiltro);
        LocalDateTime desde = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime hasta = fecha != null ? fecha.plusDays(1).atStartOfDay() : null;

        Page<Pedido> page = pedidoService.buscarPedidos(
                estadoFiltro, clienteFiltro, cajero, desde, hasta, rango[0], rango[1], pageable);

        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("cliente", clienteFiltro);
        filtros.put("cajero", cajero);
        filtros.put("estado", estadoFiltro != null ? estadoFiltro.name() : null);
        filtros.put("total", totalFiltro);
        filtros.put("fecha", fecha);

        model.addAttribute("active", "pedidos");
        model.addAttribute("pageTitle", "Pedidos");
        model.addAttribute("pedidos", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("boletas", pedidoService.getBoletasDe(page.getContent()));
        model.addAttribute("estados", EstadoPedido.values());
        model.addAttribute("cajeros", pedidoService.listarCajeros());
        model.addAttribute("rangosTotal", RANGOS_TOTAL);
        model.addAttribute("filtroCliente", clienteFiltro != null ? clienteFiltro : "");
        model.addAttribute("filtroCajero", cajero);
        model.addAttribute("filtroEstado", estadoFiltro != null ? estadoFiltro.name() : "");
        model.addAttribute("filtroTotal", totalFiltro != null ? totalFiltro : "");
        model.addAttribute("filtroFecha", fecha != null ? fecha.toString() : "");
        model.addAttribute("baseUrl", "/admin/pedidos");
        model.addAttribute("query", PageQuery.of(filtros));
        return "admin/pedidos";
    }

    private EstadoPedido parseEstadoFiltro(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoPedido.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private BigDecimal[] parseRangoTotal(String clave) {
        if (clave == null) {
            return new BigDecimal[]{null, null};
        }
        String[] partes = clave.split("-", -1);
        BigDecimal min = partes[0].isEmpty() ? null : new BigDecimal(partes[0]);
        BigDecimal max = partes.length > 1 && !partes[1].isEmpty() ? new BigDecimal(partes[1]) : null;
        return new BigDecimal[]{min, max};
    }

    private static Map<String, String> crearRangosTotal() {
        Map<String, String> rangos = new LinkedHashMap<>();
        rangos.put("0-10", "S/ 0 - 10");
        rangos.put("11-20", "S/ 11 - 20");
        rangos.put("21-30", "S/ 21 - 30");
        rangos.put("31-50", "S/ 31 - 50");
        rangos.put("51-100", "S/ 51 - 100");
        rangos.put("101-", "S/ 100+");
        return rangos;
    }

    @GetMapping("/pedidos/{id}")
    public String pedidoDetalle(@PathVariable Integer id, Model model) {
        model.addAttribute("active", "pedidos");
        model.addAttribute("pageTitle", "Detalle de pedido");
        model.addAttribute("pedido", pedidoService.getPedido(id));
        model.addAttribute("detalles", pedidoService.getDetalle(id));
        model.addAttribute("boleta", pedidoService.getBoletasPorPedido().get(id));
        return "admin/pedido-detalle";
    }

    @PostMapping("/pedidos/{id}/estado")
    public String cambiarEstadoPedido(@PathVariable Integer id,
                                      @RequestParam String estado,
                                      RedirectAttributes ra) {
        try {
            EstadoPedido nuevo = EstadoPedido.valueOf(estado.trim().toUpperCase(Locale.ROOT));
            if (nuevo == EstadoPedido.ANULADO) {
                throw new IllegalArgumentException("Para anular usa la opcion Anular (requiere motivo)");
            }
            pedidoService.actualizarEstado(id, nuevo);
            ra.addFlashAttribute("flash", "Pedido #" + id + " ahora esta " + nuevo.name().toLowerCase(Locale.ROOT) + ".");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/pedidos";
    }

    @PostMapping("/pedidos/{id}/anular")
    public String anularPedido(@PathVariable Integer id,
                               @RequestParam(required = false) String motivo,
                               RedirectAttributes ra) {
        pedidoService.anularPedido(id, motivo);
        ra.addFlashAttribute("flash", "Pedido #" + id + " anulado y stock revertido.");
        return "redirect:/admin/pedidos";
    }
}
