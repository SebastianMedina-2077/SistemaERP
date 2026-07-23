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
 * Envio de correos de marca de la tienda: comprobante (boleta electronica) y codigo
 * de verificacion de correo. Diseñado para NO romper la venta ni el registro si el
 * SMTP no esta configurado: en ese caso registra el aviso y la operacion continua.
 *
 * Estados devueltos por {@link #enviar}: ENVIADO / PENDIENTE / NO_CONFIGURADO.
 *
 * El HTML usa tablas + estilos INLINE (compatible con Gmail; sin &lt;style&gt; ni
 * clases) y la marca como texto estilizado, nunca imagenes remotas (Gmail las bloquea).
 */
@Service
public class EmailComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(EmailComprobanteService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final String ENVIADO = "ENVIADO";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String NO_CONFIGURADO = "NO_CONFIGURADO";

    // Paleta de marca Mamma Tomato.
    private static final String VERDE = "#217A3A";
    private static final String TOMATE = "#C0392B";
    private static final String CREMA = "#FAF3E7";
    private static final String HORNO = "#F5A623";
    private static final String TEXTO = "#2A2A2A";
    private static final String TENUE = "#6B6B6B";

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

    /**
     * Envia el codigo de verificacion de correo (registro de la tienda). Nunca lanza:
     * si el SMTP no esta configurado, solo registra el aviso y el registro continua.
     */
    public void enviarCodigoVerificacion(String email, String codigo) {
        if (email == null || email.isBlank()) {
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || remitente == null || remitente.isBlank()) {
            log.warn("SMTP no configurado: no se envia el codigo de verificacion a {}.", email);
            return;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(email.trim());
            helper.setSubject("Verifica tu correo - " + empresa.getNombreComercial());
            helper.setText(construirHtmlVerificacion(codigo), true);
            sender.send(mensaje);
            log.info("Codigo de verificacion enviado a {}", email);
        } catch (Exception ex) {
            log.error("Fallo el envio del codigo de verificacion a {}: {}", email, ex.getMessage());
        }
    }

    // ---- HTML de marca ---------------------------------------------

    /** Cabecera de marca reutilizable: "Mamma" en verde y "Tomato" en tomate sobre crema. */
    private String cabeceraMarca(String subtitulo) {
        return "<tr><td style='background:" + VERDE + ";padding:22px 28px;text-align:center'>"
                + "<div style='font-family:Georgia,\"Times New Roman\",serif;font-size:26px;font-weight:bold;"
                + "letter-spacing:0.5px;color:" + CREMA + "'>Mamma"
                + "<span style='color:" + HORNO + "'> Tomato</span></div>"
                + (subtitulo == null ? "" :
                "<div style='font-family:Arial,sans-serif;font-size:12px;letter-spacing:2px;"
                        + "text-transform:uppercase;color:" + CREMA + ";opacity:0.85;margin-top:4px'>"
                        + subtitulo + "</div>")
                + "</td></tr>";
    }

    /** Pie con la razon social y el RUC del emisor. */
    private String pieEmisor() {
        return "<tr><td style='background:" + CREMA + ";padding:18px 28px;text-align:center;"
                + "font-family:Arial,sans-serif;font-size:12px;color:" + TENUE + ";"
                + "border-top:2px solid " + TOMATE + "'>"
                + "<strong style='color:" + TEXTO + "'>" + escapar(empresa.getRazonSocial()) + "</strong><br>"
                + "RUC: " + escapar(empresa.getRuc()) + " &nbsp;&middot;&nbsp; " + escapar(empresa.getDireccion()) + "<br>"
                + escapar(empresa.getTelefono())
                + "</td></tr>";
    }

    private String construirHtml(Boleta boleta, List<DetallePedido> detalles) {
        StringBuilder filas = new StringBuilder();
        boolean alterna = false;
        for (DetallePedido d : detalles) {
            String nombre = d.getProducto() != null ? escapar(d.getProducto().getNombre()) : "Producto";
            String fondo = alterna ? "#FFFFFF" : "#FBF7EF";
            alterna = !alterna;
            filas.append("<tr>")
                    .append("<td style='padding:9px 12px;font-family:Arial,sans-serif;font-size:13px;color:")
                    .append(TEXTO).append(";background:").append(fondo)
                    .append(";text-align:center;border-bottom:1px solid #EFE6D6'>").append(d.getCantidad()).append("</td>")
                    .append("<td style='padding:9px 12px;font-family:Arial,sans-serif;font-size:13px;color:")
                    .append(TEXTO).append(";background:").append(fondo)
                    .append(";border-bottom:1px solid #EFE6D6'>").append(nombre).append("</td>")
                    .append("<td style='padding:9px 12px;font-family:Arial,sans-serif;font-size:13px;color:")
                    .append(TEXTO).append(";background:").append(fondo)
                    .append(";text-align:right;white-space:nowrap;border-bottom:1px solid #EFE6D6'>S/ ")
                    .append(fmt(d.getSubtotal())).append("</td>")
                    .append("</tr>");
        }

        String fecha = boleta.getPedido() != null && boleta.getPedido().getFecha() != null
                ? boleta.getPedido().getFecha().format(FECHA) : "";
        String tipo = boleta.getTipoComprobante() != null ? escapar(boleta.getTipoComprobante().getTitulo()) : "Comprobante";
        String cliente = boleta.getClienteRazonSocial() != null && !boleta.getClienteRazonSocial().isBlank()
                ? escapar(boleta.getClienteRazonSocial())
                : "Cliente";
        String documento = boleta.getClienteDocumento() != null && !boleta.getClienteDocumento().isBlank()
                ? escapar(boleta.getClienteDocumento()) : "-";

        return "<div style='background:#EDE4D3;padding:24px 0;font-family:Arial,sans-serif'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' border='0' align='center' "
                + "style='width:100%;max-width:520px;margin:0 auto;background:#FFFFFF;border-collapse:collapse;"
                + "border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08)'>"
                + cabeceraMarca(tipo)

                // Numero de comprobante y fecha.
                + "<tr><td style='padding:20px 28px 6px 28px'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='width:100%'>"
                + "<tr>"
                + "<td style='font-family:Arial,sans-serif;font-size:13px;color:" + TENUE + "'>"
                + "<span style='display:inline-block;background:" + TOMATE + ";color:#FFF;font-size:12px;"
                + "font-weight:bold;padding:4px 10px;border-radius:4px'>" + escapar(boleta.getNumeroFormateado()) + "</span>"
                + "</td>"
                + "<td style='font-family:Arial,sans-serif;font-size:12px;color:" + TENUE + ";text-align:right'>"
                + escapar(fecha) + "</td>"
                + "</tr></table></td></tr>"

                // Datos del cliente.
                + "<tr><td style='padding:6px 28px 14px 28px'>"
                + "<div style='font-family:Arial,sans-serif;font-size:12px;color:" + TENUE + "'>Cliente</div>"
                + "<div style='font-family:Arial,sans-serif;font-size:14px;color:" + TEXTO + ";font-weight:bold'>"
                + cliente + "</div>"
                + "<div style='font-family:Arial,sans-serif;font-size:12px;color:" + TENUE + "'>Doc.: " + documento + "</div>"
                + "</td></tr>"

                // Tabla de items.
                + "<tr><td style='padding:0 28px'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' border='0' "
                + "style='width:100%;border-collapse:collapse;border:1px solid #EFE6D6;border-radius:8px;overflow:hidden'>"
                + "<tr>"
                + "<th style='background:" + VERDE + ";color:#FFF;font-family:Arial,sans-serif;font-size:12px;"
                + "padding:9px 12px;text-align:center;width:40px'>Cant</th>"
                + "<th style='background:" + VERDE + ";color:#FFF;font-family:Arial,sans-serif;font-size:12px;"
                + "padding:9px 12px;text-align:left'>Producto</th>"
                + "<th style='background:" + VERDE + ";color:#FFF;font-family:Arial,sans-serif;font-size:12px;"
                + "padding:9px 12px;text-align:right'>Subtotal</th>"
                + "</tr>"
                + filas
                + "</table></td></tr>"

                // Totales.
                + "<tr><td style='padding:16px 28px 6px 28px'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='width:100%'>"
                + "<tr><td style='font-family:Arial,sans-serif;font-size:12px;color:" + TENUE + ";text-align:right'>"
                + "Op. Gravada</td><td style='font-family:Arial,sans-serif;font-size:12px;color:" + TENUE
                + ";text-align:right;width:110px'>S/ " + fmt(boleta.getSubtotal()) + "</td></tr>"
                + "<tr><td style='font-family:Arial,sans-serif;font-size:12px;color:" + TENUE + ";text-align:right'>"
                + "IGV (18%)</td><td style='font-family:Arial,sans-serif;font-size:12px;color:" + TENUE
                + ";text-align:right'>S/ " + fmt(boleta.getIgv()) + "</td></tr>"
                + "</table></td></tr>"

                + "<tr><td style='padding:4px 28px 20px 28px'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='width:100%;"
                + "background:" + CREMA + ";border-radius:8px'>"
                + "<tr><td style='font-family:Arial,sans-serif;font-size:15px;font-weight:bold;color:" + TEXTO
                + ";padding:12px 14px'>TOTAL A PAGAR</td>"
                + "<td style='font-family:Arial,sans-serif;font-size:20px;font-weight:bold;color:" + TOMATE
                + ";padding:12px 14px;text-align:right'>S/ " + fmt(boleta.getTotal()) + "</td></tr>"
                + "</table>"
                + "<div style='font-family:Arial,sans-serif;font-size:11px;color:" + TENUE
                + ";text-align:right;margin-top:6px'>Precios con IGV incluido.</div>"
                + "</td></tr>"

                // Agradecimiento.
                + "<tr><td style='padding:0 28px 20px 28px;text-align:center;font-family:Arial,sans-serif;"
                + "font-size:13px;color:" + VERDE + ";font-weight:bold'>Gracias por tu compra</td></tr>"

                + pieEmisor()
                + "</table></div>";
    }

    private String construirHtmlVerificacion(String codigo) {
        return "<div style='background:#EDE4D3;padding:24px 0;font-family:Arial,sans-serif'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' border='0' align='center' "
                + "style='width:100%;max-width:480px;margin:0 auto;background:#FFFFFF;border-collapse:collapse;"
                + "border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08)'>"
                + cabeceraMarca("Verifica tu correo")

                + "<tr><td style='padding:26px 32px 8px 32px;text-align:center;font-family:Arial,sans-serif;"
                + "font-size:15px;color:" + TEXTO + "'>Hola, gracias por registrarte.</td></tr>"
                + "<tr><td style='padding:0 32px 8px 32px;text-align:center;font-family:Arial,sans-serif;"
                + "font-size:13px;color:" + TENUE + "'>Usa este codigo para confirmar tu correo:</td></tr>"

                // Codigo grande.
                + "<tr><td style='padding:10px 32px 6px 32px;text-align:center'>"
                + "<div style='display:inline-block;background:" + CREMA + ";border:2px dashed " + HORNO + ";"
                + "border-radius:10px;padding:16px 28px;font-family:\"Courier New\",monospace;font-size:34px;"
                + "font-weight:bold;letter-spacing:8px;color:" + TOMATE + "'>" + escapar(codigo) + "</div>"
                + "</td></tr>"

                + "<tr><td style='padding:6px 32px 22px 32px;text-align:center;font-family:Arial,sans-serif;"
                + "font-size:12px;color:" + TENUE + "'>El codigo vence en <strong>10 minutos</strong>. "
                + "Si no creaste esta cuenta, ignora este correo.</td></tr>"

                + pieEmisor()
                + "</table></div>";
    }

    private String fmt(BigDecimal v) {
        return v != null ? v.toPlainString() : "0.00";
    }

    /** Escapa lo minimo para no romper el HTML con datos del cliente/producto. */
    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
