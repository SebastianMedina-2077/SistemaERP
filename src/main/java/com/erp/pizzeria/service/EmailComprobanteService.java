package com.erp.pizzeria.service;

import com.erp.pizzeria.config.EmpresaProperties;
import com.erp.pizzeria.model.Boleta;
import com.erp.pizzeria.model.DetallePedido;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Envio del comprobante por email (boleta electronica). Diseñado para NO romper la
 * venta si el SMTP no esta configurado: en ese caso registra el comprobante como
 * pendiente y la operacion continua con normalidad.
 *
 * Estados devueltos: ENVIADO / PENDIENTE / NO_CONFIGURADO.
 */
@Service
public class EmailComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(EmailComprobanteService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final String ENVIADO = "ENVIADO";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String NO_CONFIGURADO = "NO_CONFIGURADO";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmpresaProperties empresa;
    private final String remitente;

    public EmailComprobanteService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                   EmpresaProperties empresa,
                                   @Value("${spring.mail.username:}") String remitente) {
        this.mailSenderProvider = mailSenderProvider;
        this.empresa = empresa;
        this.remitente = remitente;
    }

    /**
     * Intenta enviar el comprobante al email indicado. Nunca lanza: ante cualquier
     * problema devuelve PENDIENTE/NO_CONFIGURADO para que la venta no falle.
     */
    public String enviar(Boleta boleta, List<DetallePedido> detalles, String email) {
        if (email == null || email.isBlank()) {
            return NO_CONFIGURADO;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || remitente == null || remitente.isBlank()) {
            log.warn("SMTP no configurado: el comprobante {} para {} queda PENDIENTE de envio.",
                    boleta.getNumeroFormateado(), email);
            return NO_CONFIGURADO;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(email.trim());
            helper.setSubject(empresa.getNombreComercial() + " - Comprobante " + boleta.getNumeroFormateado());
            helper.setText(construirHtml(boleta, detalles), true);
            sender.send(mensaje);
            log.info("Comprobante {} enviado por email a {}", boleta.getNumeroFormateado(), email);
            return ENVIADO;
        } catch (Exception ex) {
            log.error("Fallo el envio del comprobante {} a {}: {}",
                    boleta.getNumeroFormateado(), email, ex.getMessage());
            return PENDIENTE;
        }
    }

    private String construirHtml(Boleta boleta, List<DetallePedido> detalles) {
        StringBuilder filas = new StringBuilder();
        for (DetallePedido d : detalles) {
            String nombre = d.getProducto() != null ? d.getProducto().getNombre() : "Producto";
            filas.append("<tr><td>").append(d.getCantidad()).append("</td><td>")
                    .append(nombre).append("</td><td style='text-align:right'>S/ ")
                    .append(fmt(d.getSubtotal())).append("</td></tr>");
        }
        String fecha = boleta.getPedido() != null && boleta.getPedido().getFecha() != null
                ? boleta.getPedido().getFecha().format(FECHA) : "";
        return "<div style='font-family:Arial,sans-serif;max-width:480px;margin:auto'>"
                + "<h2 style='text-align:center;color:#c0392b'>" + empresa.getNombreComercial() + "</h2>"
                + "<p style='text-align:center'>" + empresa.getRazonSocial()
                + "<br>RUC: " + empresa.getRuc() + "</p>"
                + "<h3 style='text-align:center'>" + boleta.getTipoComprobante().getTitulo()
                + "<br>" + boleta.getNumeroFormateado() + "</h3>"
                + "<p>Fecha: " + fecha + "</p>"
                + "<table style='width:100%;border-collapse:collapse' border='0'>"
                + "<tr><th align='left'>Cant</th><th align='left'>Descripcion</th><th align='right'>Importe</th></tr>"
                + filas
                + "</table><hr>"
                + "<p style='text-align:right'>Op. Gravada: S/ " + fmt(boleta.getSubtotal())
                + "<br>IGV (18%): S/ " + fmt(boleta.getIgv())
                + "<br><strong>TOTAL: S/ " + fmt(boleta.getTotal()) + "</strong></p>"
                + "<p style='text-align:center;color:#666'>Gracias por su compra.</p>"
                + "</div>";
    }

    private String fmt(BigDecimal v) {
        return v != null ? v.toPlainString() : "0.00";
    }
}
