package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByEstado(EstadoPedido estado);
    List<Pedido> findByEstadoInOrderByFechaAsc(List<EstadoPedido> estados);
    boolean existsByUsuario_IdUsuario(Integer idUsuario);

    @Query(value = """
            select distinct p from Pedido p
            left join Boleta b on b.pedido = p
            where (:numero is null or p.idPedido = :numero)
              and (:estado is null or p.estado = :estado)
              and (:cliente is null or lower(p.cliente.nombre) like lower(concat('%', :cliente, '%')))
              and (:cajero is null or p.usuario.idUsuario = :cajero)
              and (:fechaDesde is null or p.fecha >= :fechaDesde)
              and (:fechaHasta is null or p.fecha < :fechaHasta)
              and (:totalMin is null or b.total >= :totalMin)
              and (:totalMax is null or b.total <= :totalMax)
            """,
            countQuery = """
            select count(distinct p) from Pedido p
            left join Boleta b on b.pedido = p
            where (:numero is null or p.idPedido = :numero)
              and (:estado is null or p.estado = :estado)
              and (:cliente is null or lower(p.cliente.nombre) like lower(concat('%', :cliente, '%')))
              and (:cajero is null or p.usuario.idUsuario = :cajero)
              and (:fechaDesde is null or p.fecha >= :fechaDesde)
              and (:fechaHasta is null or p.fecha < :fechaHasta)
              and (:totalMin is null or b.total >= :totalMin)
              and (:totalMax is null or b.total <= :totalMax)
            """)
    Page<Pedido> buscar(@Param("numero") Integer numero,
                        @Param("estado") EstadoPedido estado,
                        @Param("cliente") String cliente,
                        @Param("cajero") Integer cajero,
                        @Param("fechaDesde") LocalDateTime fechaDesde,
                        @Param("fechaHasta") LocalDateTime fechaHasta,
                        @Param("totalMin") BigDecimal totalMin,
                        @Param("totalMax") BigDecimal totalMax,
                        Pageable pageable);

    @Query("select distinct p.usuario from Pedido p order by p.usuario.username")
    List<Usuario> findCajeros();

    @Query("select count(p) from Pedido p where p.fecha >= :desde and p.fecha < :hasta")
    long contarPorRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select coalesce(sum(b.total), 0) from Boleta b where b.pedido.fecha >= :desde and b.pedido.fecha < :hasta")
    BigDecimal sumarVentasPorRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
