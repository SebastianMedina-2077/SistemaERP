package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.TopProductoDTO;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.service.CompraService;
import com.erp.pizzeria.service.InventarioService;
import com.erp.pizzeria.service.PedidoService;
import com.erp.pizzeria.service.ReporteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

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

    @GetMapping("/pedidos")
    public String pedidos(Model model) {
        model.addAttribute("active", "pedidos");
        model.addAttribute("pageTitle", "Pedidos");
        model.addAttribute("pedidos", pedidoService.listPedidos());
        model.addAttribute("boletas", pedidoService.getBoletasPorPedido());
        return "admin/pedidos";
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
