package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.BoletaDTO;
import com.erp.pizzeria.dto.DetallePedidoDTO;
import com.erp.pizzeria.dto.PedidoDTO;
import com.erp.pizzeria.exception.StockInsuficienteException;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.repository.BoletaRepository;
import com.erp.pizzeria.repository.InsumoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica la logica transaccional de pedidos contra la BD real (seed cargado).
 * Cada test corre en su propia transaccion y hace rollback al terminar.
 */
@SpringBootTest
@Transactional
class PedidoServiceIntegrationTest {

    private static final Integer ID_CAJERO = 2;
    private static final Integer ID_EFECTIVO = 1;
    private static final Integer ID_PIZZA_AMERICANA = 1; // receta: queso 0.5, salsa 0.25, masa 1.0

    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private CatalogService catalogService;
    @Autowired
    private InsumoRepository insumoRepository;
    @Autowired
    private BoletaRepository boletaRepository;

    private BigDecimal stock(Integer idInsumo) {
        return insumoRepository.findById(idInsumo).map(Insumo::getStock).orElseThrow();
    }

    private PedidoDTO pedidoDe(int cantidad, String observacion) {
        PedidoDTO dto = new PedidoDTO();
        dto.setClienteNombre("Cliente Test");
        dto.setClienteTelefono("999000111");
        dto.setIdMetodoPago(ID_EFECTIVO);
        dto.setItems(List.of(new DetallePedidoDTO(ID_PIZZA_AMERICANA, cantidad, observacion)));
        return dto;
    }

    @Test
    void crearPedido_descuentaStockYGeneraBoleta() {
        BigDecimal quesoAntes = stock(1);
        BigDecimal salsaAntes = stock(2);
        BigDecimal masaAntes = stock(3);
        long boletasAntes = boletaRepository.count();

        PedidoDTO dto = pedidoDe(1, "Sin aceitunas");

        BoletaDTO boleta = pedidoService.crearPedido(dto, ID_CAJERO);

        // Precios con IGV INCLUIDO: el total es el precio vigente del producto y el
        // impuesto se extrae del total (igv = total x 18/118, redondeo HALF_UP).
        BigDecimal precio = catalogService.getProducto(ID_PIZZA_AMERICANA).getPrecio();
        BigDecimal igv = precio.multiply(new BigDecimal("0.18"))
                .divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        assertThat(boleta.getTotal()).isEqualByComparingTo(precio);
        assertThat(boleta.getIgv()).isEqualByComparingTo(igv);
        assertThat(boleta.getSubtotal()).isEqualByComparingTo(precio.subtract(igv));
        assertThat(boleta.getNumeroBoleta()).startsWith("B001-");
        assertThat(boletaRepository.count()).isEqualTo(boletasAntes + 1);

        // Stock de insumos descontado segun la receta
        assertThat(stock(1)).isEqualByComparingTo(quesoAntes.subtract(new BigDecimal("0.500")));
        assertThat(stock(2)).isEqualByComparingTo(salsaAntes.subtract(new BigDecimal("0.250")));
        assertThat(stock(3)).isEqualByComparingTo(masaAntes.subtract(new BigDecimal("1.000")));

        // El pedido nace en estado PENDIENTE
        Pedido pedido = pedidoService.getPedido(boleta.getIdPedido());
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
    }

    @Test
    void crearPedido_sinStockSuficiente_lanzaExcepcionYNoTocaStock() {
        BigDecimal quesoAntes = stock(1);

        // 100 pizzas requieren mucho mas queso/masa del disponible
        PedidoDTO dto = pedidoDe(100, null);

        assertThatThrownBy(() -> pedidoService.crearPedido(dto, ID_CAJERO))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Stock insuficiente");

        // La verificacion ocurre antes de cualquier escritura: stock intacto
        assertThat(stock(1)).isEqualByComparingTo(quesoAntes);
    }
}
