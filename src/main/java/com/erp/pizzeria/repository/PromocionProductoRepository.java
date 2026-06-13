package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.PromocionProducto;
import com.erp.pizzeria.model.id.PromocionProductoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromocionProductoRepository extends JpaRepository<PromocionProducto, PromocionProductoId> {
    List<PromocionProducto> findByProducto_IdProducto(Integer idProducto);
    List<PromocionProducto> findByPromocion_IdPromocion(Integer idPromocion);
}
