package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Pago;
import com.erp.pizzeria.model.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    /** Total cobrado por metodo de pago en el turno de un cajero (para el cuadre de caja). */
    @Query("SELECT p.metodoPago.descripcion, SUM(p.monto) FROM Pago p "
            + "WHERE p.pedido.usuario.idUsuario = :cajeroId AND p.pedido.fecha >= :desde "
            + "AND p.pedido.estado <> :anulado GROUP BY p.metodoPago.descripcion")
    List<Object[]> resumenPorMetodo(@Param("cajeroId") Integer cajeroId,
                                    @Param("desde") LocalDateTime desde,
                                    @Param("anulado") EstadoPedido anulado);
}
