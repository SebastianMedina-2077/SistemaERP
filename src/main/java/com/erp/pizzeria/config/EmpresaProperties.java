package com.erp.pizzeria.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Datos fiscales de la empresa que se imprimen en la boleta y se codifican en el QR
 * (formato de comprobante electronico SUNAT). Se configuran con el prefijo "empresa.*"
 * en application.properties.
 */
@Component
@ConfigurationProperties(prefix = "empresa")
@Getter
@Setter
public class EmpresaProperties {

    /** RUC de la empresa (11 digitos). Placeholder hasta configurar el real. */
    private String ruc = "00000000000";

    /** Razon social registrada en SUNAT (placeholder). */
    private String razonSocial = "MAMA TOMATO S.A.C.";

    /** Nombre comercial mostrado como marca en el encabezado de la boleta. */
    private String nombreComercial = "Mamma Tomato";

    /** Direccion del local impreso en la boleta. */
    private String direccion = "Av. Los Olivos 123 - Lima, Peru";

    /** Telefono de contacto impreso en la boleta. */
    private String telefono = "(01) 555-1234";

    /** Tipo de documento del adquiriente (0 = sin documento / consumidor final). */
    private String tipoDocAdquiriente = "0";

    /** Numero de documento del adquiriente (0 cuando es consumidor final). */
    private String numDocAdquiriente = "0";
}