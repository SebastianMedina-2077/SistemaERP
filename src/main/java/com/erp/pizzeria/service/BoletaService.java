package com.erp.pizzeria.service;

import com.erp.pizzeria.config.EmpresaProperties;
import com.erp.pizzeria.dto.BoletaImpresionDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Boleta;
import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.Pago;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.TipoComprobante;
import com.erp.pizzeria.repository.BoletaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Arma el modelo de impresion de la boleta (tipo ticket) a partir del pedido:
 * datos de empresa, detalle, importes, desglose de pagos y el QR con formato SUNAT.
 */
@Service
@Transactional(readOnly = true)
public class BoletaService {

    private static final DateTimeFormatter FECHA_QR = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BoletaRepository boletaRepository;
    private final PedidoService pedidoService;
    private final GeneradorQrService generadorQrService;
    private final EmpresaProperties empresa;

    public BoletaService(BoletaRepository boletaRepository,
                         PedidoService pedidoService,
                         GeneradorQrService generadorQrService,
                         EmpresaProperties empresa) {
        this.boletaRepository = boletaRepository;
        this.pedidoService = pedidoService;
        this.generadorQrService = generadorQrService;
        this.empresa = empresa;
    }

    /** Construye el ticket imprimible de la boleta asociada a un pedido. */
    public BoletaImpresionDTO construirImpresion(Integer idPedido) {
        Boleta boleta = boletaRepository.findByPedido_IdPedido(idPedido)
                .orElseThrow(() -> ResourceNotFoundException.of("Boleta del pedido", idPedido));
        Pedido pedido = boleta.getPedido();
        TipoComprobante tipo = boleta.getTipoComprobante() != null
                ? boleta.getTipoComprobante() : TipoComprobante.BOLETA;
        boolean esFactura = tipo == TipoComprobante.FACTURA;

        String serie = boleta.getSerie() != null ? boleta.getSerie() : tipo.getSerie();
        String correlativo = String.format("%06d", boleta.getCorrelativo() != null ? boleta.getCorrelativo() : 0);
        String numero = boleta.getNumeroFormateado();

        List<BoletaImpresionDTO.Linea> items = pedidoService.getDetalle(idPedido).stream()
                .map(this::aLinea)
                .toList();

        List<BoletaImpresionDTO.Pago> pagos = pedidoService.getPagos(idPedido).stream()
                .map(this::aPago)
                .toList();

        String qrContenido = construirCadenaSunat(boleta, tipo, serie, correlativo);
        String qrDataUri = generadorQrService.generarDataUri(qrContenido);

        // Adquiriente impreso: en factura, RUC + razon social; en boleta, DNI si lo hay.
        String docAdquiriente = boleta.getClienteDocumento();
        String razonAdquiriente = esFactura ? boleta.getClienteRazonSocial() : null;
        String nombreCliente = esFactura && razonAdquiriente != null
                ? razonAdquiriente
                : (pedido.getCliente() != null ? pedido.getCliente().getNombre() : "Consumidor final");

        return BoletaImpresionDTO.builder()
                .nombreComercial(empresa.getNombreComercial())
                .razonSocial(empresa.getRazonSocial())
                .ruc(empresa.getRuc())
                .direccion(empresa.getDireccion())
                .telefono(empresa.getTelefono())
                .titulo(tipo.getTitulo())
                .factura(esFactura)
                .numero(numero)
                .fecha(pedido.getFecha())
                .cajero(nombreCajero(pedido.getUsuario()))
                .cliente(nombreCliente)
                .clienteDocumento(docAdquiriente)
                .clienteRazonSocial(razonAdquiriente)
                .mesa(boleta.getMesa())
                .subtotal(boleta.getSubtotal())
                .igv(boleta.getIgv())
                .total(boleta.getTotal())
                .items(items)
                .pagos(pagos)
                .qrContenido(qrContenido)
                .qrDataUri(qrDataUri)
                .build();
    }

    /**
     * Cadena del QR con formato de comprobante electronico SUNAT, separada por "|":
     * RUC | tipoComprobante | serie | correlativo | IGV | total | fecha | tipoDocAdq | numDocAdq
     * En factura, el adquiriente es el RUC del cliente (tipoDoc 6); en boleta, el DNI (1) o
     * el generico configurado si no hay documento.
     */
    private String construirCadenaSunat(Boleta boleta, TipoComprobante tipo, String serie, String correlativo) {
        BigDecimal igv = boleta.getIgv() != null ? boleta.getIgv() : BigDecimal.ZERO;
        String fecha = boleta.getPedido().getFecha() != null
                ? boleta.getPedido().getFecha().format(FECHA_QR)
                : "";

        String tipoDocAdq = empresa.getTipoDocAdquiriente();
        String numDocAdq = empresa.getNumDocAdquiriente();
        if (tipo == TipoComprobante.FACTURA && boleta.getClienteDocumento() != null) {
            tipoDocAdq = "6"; // RUC
            numDocAdq = boleta.getClienteDocumento();
        } else if (boleta.getClienteDocumento() != null) {
            tipoDocAdq = "1"; // DNI
            numDocAdq = boleta.getClienteDocumento();
        }

        return String.join("|",
                empresa.getRuc(),
                tipo.getCodigoSunat(),
                serie,
                correlativo,
                igv.toPlainString(),
                boleta.getTotal().toPlainString(),
                fecha,
                tipoDocAdq,
                numDocAdq);
    }

    private BoletaImpresionDTO.Linea aLinea(DetallePedido d) {
        return BoletaImpresionDTO.Linea.builder()
                .nombre(d.getProducto() != null ? d.getProducto().getNombre() : "Producto")
                .cantidad(d.getCantidad())
                .precioUnitario(d.getPrecioUnitario())
                .importe(d.getSubtotal())
                .build();
    }

    private BoletaImpresionDTO.Pago aPago(Pago p) {
        return BoletaImpresionDTO.Pago.builder()
                .metodo(p.getMetodoPago() != null ? p.getMetodoPago().getDescripcion() : "Pago")
                .monto(p.getMonto())
                .build();
    }

    private String nombreCajero(Usuario u) {
        if (u == null) return "";
        return u.getEmpleado() != null
                ? u.getEmpleado().getNombre() + " " + u.getEmpleado().getApellidoPaterno() + " " + u.getEmpleado().getApellidoMaterno()
                : u.getUsername();
    }
}