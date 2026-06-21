package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.AnuladoReporteDTO;
import com.erp.pizzeria.dto.ProveedorReporteDTO;
import com.erp.pizzeria.dto.ReporteDataDTO;
import com.erp.pizzeria.dto.TipoMovReporteDTO;
import com.erp.pizzeria.dto.TopProductoDTO;
import com.erp.pizzeria.model.Boleta;
import com.erp.pizzeria.model.Compra;
import com.erp.pizzeria.model.Movimiento;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.service.CompraService;
import com.erp.pizzeria.service.InventarioService;
import com.erp.pizzeria.service.PedidoService;
import com.erp.pizzeria.service.ReporteExportService;
import com.erp.pizzeria.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class ReporteController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final ReporteService reporteService;
    private final ReporteExportService exportService;
    private final CompraService compraService;
    private final InventarioService inventarioService;
    private final PedidoService pedidoService;

    public ReporteController(ReporteService reporteService,
                             ReporteExportService exportService,
                             CompraService compraService,
                             InventarioService inventarioService,
                             PedidoService pedidoService) {
        this.reporteService = reporteService;
        this.exportService = exportService;
        this.compraService = compraService;
        this.inventarioService = inventarioService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/reportes")
    public String reportes(Model model) {
        ReporteDataDTO data = buildReportData();
        long topMax = data.getTopProductos().stream().mapToLong(TopProductoDTO::getCantidad).max().orElse(1);

        model.addAttribute("active", "reportes");
        model.addAttribute("pageTitle", "Reportes");
        model.addAttribute("salesStats", data.getSalesStats());
        model.addAttribute("topProductos", data.getTopProductos());
        model.addAttribute("topMax", topMax);
        model.addAttribute("comprasPorProveedor", data.getComprasPorProveedor());
        model.addAttribute("movimientosPorTipo", data.getMovimientosPorTipo());
        model.addAttribute("anulados", data.getAnulados());
        return "admin/reportes";
    }

    @GetMapping("/reportes/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        byte[] pdf = exportService.buildPdf(buildReportData());
        return download(pdf, MediaType.APPLICATION_PDF, "reporte-mammatomato-" + fileStamp() + ".pdf");
    }

    @GetMapping("/reportes/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] xlsx = exportService.buildExcel(buildReportData());
        MediaType xlsxType = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return download(xlsx, xlsxType, "reporte-mammatomato-" + fileStamp() + ".xlsx");
    }

    private ResponseEntity<byte[]> download(byte[] body, MediaType type, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(type);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(body.length);
        return new ResponseEntity<>(body, headers, org.springframework.http.HttpStatus.OK);
    }

    private String fileStamp() {
        return LocalDateTime.now().format(FILE_STAMP);
    }

    /** Ensambla todos los datasets del panel; reutilizado por la vista y la exportacion. */
    private ReporteDataDTO buildReportData() {
        return new ReporteDataDTO(
                LocalDateTime.now().format(STAMP),
                reporteService.getSalesStats(),
                reporteService.getTopProductos(),
                movimientosPorTipo(),
                comprasPorProveedor(),
                anulados());
    }

    private List<AnuladoReporteDTO> anulados() {
        Map<Integer, Boleta> boletas = pedidoService.getBoletasPorPedido();
        List<Pedido> pedidos = pedidoService.listPedidos().stream()
                .filter(p -> p.getEstado() == EstadoPedido.ANULADO)
                .toList();
        List<AnuladoReporteDTO> filas = new ArrayList<>();
        int numero = 1;
        for (Pedido p : pedidos) {
            Boleta b = boletas.get(p.getIdPedido());
            String cajero = p.getUsuario().getEmpleado() != null
                    ? p.getUsuario().getEmpleado().getNombre()
                    : p.getUsuario().getUsername();
            filas.add(new AnuladoReporteDTO(
                    numero++,
                    p.getFecha() != null ? p.getFecha().format(FECHA) : "-",
                    p.getCliente().getNombre(),
                    cajero,
                    b != null ? b.getTotal() : null,
                    p.getMotivoAnulacion() != null ? p.getMotivoAnulacion() : "-"));
        }
        return filas;
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
