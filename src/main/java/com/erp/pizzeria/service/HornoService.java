package com.erp.pizzeria.service;

import com.erp.pizzeria.model.Categoria;
import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.model.enums.Tamanio;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estima el tiempo hasta que un pedido esta listo simulando la cola del horno.
 *
 * <p>No es un calculo exacto al segundo: es una aproximacion robusta pensada para
 * el tablero de cocina (KDS). Simula una linea de tiempo FIFO (por fecha del pedido)
 * en la que las pizzas compiten por los slots del horno, mientras las preparaciones
 * que no van al horno corren en paralelo.
 *
 * <p><b>Modelo de criterio</b> (constantes ajustables mas abajo):
 * <ul>
 *   <li><b>Pizzas</b> -> HORNO. Ocupan slots segun tamano (personal/mediano=1, familiar=2),
 *       hornean {@link #MINUTOS_HORNEADO} min por tanda y llevan {@link #MINUTOS_ARMADO_PIZZA}
 *       min de armado previo en paralelo (no ocupa horno).</li>
 *   <li><b>Panizzas / Complementos</b> -> preparacion en paralelo de {@link #MINUTOS_PREP_NO_HORNO}
 *       min, sin ocupar slots del horno.</li>
 *   <li><b>Bebidas</b> y cualquier categoria no reconocida como preparable -> 0 min.</li>
 * </ul>
 *
 * <p>El horno tiene {@link #CAPACIDAD_HORNO} slots simultaneos. El tiempo del pedido es
 * cuando termina de hornear su ultima pizza (o su ultima preparacion no-horno, si fuera
 * mayor). Para los PREPARANDO se descuenta el tiempo ya transcurrido desde su fecha,
 * sin bajar de 0.
 */
@Service
public class HornoService {

    // ---- Constantes del modelo (ajustables) ------------------------

    /** Slots del horno que pueden estar horneando a la vez. */
    public static final int CAPACIDAD_HORNO = 4;
    /** Minutos que dura una tanda de horneado. */
    public static final int MINUTOS_HORNEADO = 12;
    /** Minutos de armado de una pizza antes de entrar al horno (en paralelo, no ocupa slot). */
    public static final int MINUTOS_ARMADO_PIZZA = 4;
    /** Minutos de preparacion de items que no van al horno (panizzas, complementos), en paralelo. */
    public static final int MINUTOS_PREP_NO_HORNO = 6;

    // Nombres de categoria (deben coincidir con la tabla `categoria`).
    private static final String CAT_PIZZAS = "Pizzas";
    private static final String CAT_PANIZZAS = "Panizzas";
    private static final String CAT_COMPLEMENTOS = "Complementos";

    /**
     * Estima, en minutos desde ahora, cuando estara listo cada pedido de la cola.
     *
     * @param pedidosFifo       pedidos activos ordenados FIFO por fecha (los mas antiguos primero)
     * @param detallesPorPedido detalles de cada pedido (clave: idPedido)
     * @return mapa idPedido -> minutos estimados hasta estar listo (>= 0)
     */
    public Map<Integer, Integer> estimarTiempos(List<Pedido> pedidosFifo,
                                                 Map<Integer, List<DetallePedido>> detallesPorPedido) {
        LocalDateTime ahora = LocalDateTime.now();
        // Momento (en minutos, relativo a ahora) en que cada slot del horno queda libre.
        double[] slotLibre = new double[CAPACIDAD_HORNO];
        Arrays.fill(slotLibre, 0.0);

        Map<Integer, Integer> resultado = new LinkedHashMap<>();
        for (Pedido pedido : pedidosFifo) {
            List<DetallePedido> detalles = detallesPorPedido.getOrDefault(pedido.getIdPedido(), List.of());
            double fin = estimarPedido(pedido, detalles, ahora, slotLibre);
            resultado.put(pedido.getIdPedido(), (int) Math.max(0, Math.round(fin)));
        }
        return resultado;
    }

    /**
     * Simula un pedido sobre el estado actual del horno y devuelve el instante (en minutos
     * relativos a ahora) en que queda listo. Muta {@code slotLibre} al ocupar el horno.
     */
    private double estimarPedido(Pedido pedido, List<DetallePedido> detalles,
                                 LocalDateTime ahora, double[] slotLibre) {
        // Inicio de cocina del pedido (relativo a ahora): los PREPARANDO ya llevan tiempo.
        double inicio = inicioRelativo(pedido, ahora);
        double fin = inicio; // si solo lleva bebidas, esta "listo" al inicio

        // Las pizzas pueden entrar al horno cuando termina su armado (en paralelo).
        double listoParaHornear = inicio + MINUTOS_ARMADO_PIZZA;

        for (DetallePedido detalle : detalles) {
            String categoria = nombreCategoria(detalle.getProducto());
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            if (cantidad <= 0) {
                continue;
            }
            if (CAT_PIZZAS.equalsIgnoreCase(categoria)) {
                int slots = slotsPorTamano(detalle.getProducto());
                // Una linea con cantidad N son N pizzas independientes.
                for (int i = 0; i < cantidad; i++) {
                    double finPizza = hornear(slots, listoParaHornear, slotLibre);
                    fin = Math.max(fin, finPizza);
                }
            } else if (CAT_PANIZZAS.equalsIgnoreCase(categoria) || CAT_COMPLEMENTOS.equalsIgnoreCase(categoria)) {
                // Preparacion en paralelo: no depende de cantidad ni ocupa horno.
                fin = Math.max(fin, inicio + MINUTOS_PREP_NO_HORNO);
            }
            // Bebidas / no reconocido: 0 min (no altera `fin`).
        }
        return fin;
    }

    /**
     * Hornea una pizza que necesita {@code slots} slots simultaneos durante una tanda.
     * Toma los slots que se liberan antes; empieza cuando la pizza esta armada y esos
     * slots estan libres. Devuelve el instante en que termina y actualiza el horno.
     */
    private double hornear(int slots, double listoParaHornear, double[] slotLibre) {
        slots = Math.min(slots, slotLibre.length);
        // Indices de los slots que se liberan mas temprano.
        Integer[] orden = new Integer[slotLibre.length];
        for (int i = 0; i < orden.length; i++) {
            orden[i] = i;
        }
        Arrays.sort(orden, (a, b) -> Double.compare(slotLibre[a], slotLibre[b]));

        // Necesita `slots` libres a la vez: arranca cuando el ultimo de ellos se libera.
        double disponible = 0.0;
        for (int i = 0; i < slots; i++) {
            disponible = Math.max(disponible, slotLibre[orden[i]]);
        }
        double inicioHorneado = Math.max(listoParaHornear, disponible);
        double finHorneado = inicioHorneado + MINUTOS_HORNEADO;
        for (int i = 0; i < slots; i++) {
            slotLibre[orden[i]] = finHorneado;
        }
        return finHorneado;
    }

    /** Inicio de cocina del pedido relativo a ahora: 0 si acaba de entrar; negativo si ya cocina. */
    private double inicioRelativo(Pedido pedido, LocalDateTime ahora) {
        if (pedido.getFecha() == null) {
            return 0.0;
        }
        long transcurrido = Duration.between(pedido.getFecha(), ahora).toMinutes();
        if (transcurrido <= 0) {
            return 0.0;
        }
        // Solo los que ya se preparan descuentan lo avanzado; los pendientes arrancan ahora.
        boolean enPreparacion = pedido.getEstado() != null
                && "PREPARANDO".equals(pedido.getEstado().name());
        return enPreparacion ? -(double) transcurrido : 0.0;
    }

    /** Slots de horno que ocupa una pizza segun su tamano (familiar ocupa 2). */
    private int slotsPorTamano(Producto producto) {
        Tamanio tamanio = producto != null ? producto.getTamanio() : null;
        if (tamanio == Tamanio.familiar) {
            return 2;
        }
        return 1; // personal, mediano o sin tamano
    }

    private String nombreCategoria(Producto producto) {
        if (producto == null) {
            return null;
        }
        Categoria categoria = producto.getCategoria();
        return categoria != null ? categoria.getNombre() : null;
    }
}
