package com.erp.pizzeria.model;

import com.erp.pizzeria.model.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "boleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boleta")
    private Integer idBoleta;

    @Column(name = "subtotal", precision = 6, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "igv", precision = 8, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", precision = 8, scale = 2, nullable = false)
    private BigDecimal total;

    // ---- Datos del comprobante (numeracion oficial sin huecos) ----

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", length = 20, nullable = false)
    private TipoComprobante tipoComprobante = TipoComprobante.BOLETA;

    /** Serie del comprobante (B001 boleta, F001 factura). */
    @Column(name = "serie", length = 4, nullable = false)
    private String serie;

    /** Correlativo secuencial y sin huecos dentro de la serie (numeracion de negocio). */
    @Column(name = "correlativo", nullable = false)
    private Integer correlativo;

    /** Documento del cliente: DNI (boleta) o RUC (factura). Opcional en boleta. */
    @Column(name = "cliente_documento", length = 11)
    private String clienteDocumento;

    /** Razon social del adquiriente (solo factura). */
    @Column(name = "cliente_razon_social", length = 120)
    private String clienteRazonSocial;

    /** Email del cliente (solo boleta electronica). */
    @Column(name = "cliente_email", length = 120)
    private String clienteEmail;

    /** Mesa o ubicacion del pedido (BARRA / MESA-1.. / null para llevar). */
    @Column(name = "mesa", length = 20)
    private String mesa;

    /** Estado del envio por email: ENVIADO / PENDIENTE / NO_CONFIGURADO / NO_APLICA. */
    @Column(name = "email_estado", length = 20)
    private String emailEstado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_metodopago", nullable = false)
    private MetodoPago metodoPago;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    /** Numero formateado del comprobante, p. ej. "B001-000123". */
    @Transient
    public String getNumeroFormateado() {
        String s = serie != null ? serie : "B001";
        int n = correlativo != null ? correlativo : 0;
        return String.format("%s-%06d", s, n);
    }
}
