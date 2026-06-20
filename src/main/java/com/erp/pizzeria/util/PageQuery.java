package com.erp.pizzeria.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Construye el sufijo de query string ("&k=v&...") con los filtros activos para
 * conservarlos en los enlaces de paginacion. Omite valores nulos o en blanco.
 */
public final class PageQuery {

    private PageQuery() {
    }

    public static String of(Map<String, ?> filtros) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> e : filtros.entrySet()) {
            Object valor = e.getValue();
            if (valor == null || valor.toString().isBlank()) {
                continue;
            }
            sb.append('&').append(e.getKey()).append('=')
              .append(URLEncoder.encode(valor.toString(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
