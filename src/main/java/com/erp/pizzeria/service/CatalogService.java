package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.ProductoDTO;
import com.erp.pizzeria.dto.ProductoFormDTO;
import com.erp.pizzeria.dto.PromocionFormDTO;
import com.erp.pizzeria.model.id.PromocionProductoId;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Categoria;
import com.erp.pizzeria.model.ComboProducto;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.model.enums.Tamanio;
import com.erp.pizzeria.model.ProductoInsumo;
import com.erp.pizzeria.model.Promocion;
import com.erp.pizzeria.model.PromocionProducto;
import com.erp.pizzeria.repository.CategoriaRepository;
import com.erp.pizzeria.repository.ComboProductoRepository;
import com.erp.pizzeria.repository.DetallePedidoRepository;
import com.erp.pizzeria.repository.ProductoInsumoRepository;
import com.erp.pizzeria.repository.ProductoRepository;
import com.erp.pizzeria.repository.PromocionProductoRepository;
import com.erp.pizzeria.repository.PromocionRepository;
import com.erp.pizzeria.util.CodigoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoInsumoRepository productoInsumoRepository;
    private final ComboProductoRepository comboProductoRepository;
    private final PromocionRepository promocionRepository;
    private final PromocionProductoRepository promocionProductoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public CatalogService(ProductoRepository productoRepository,
                          CategoriaRepository categoriaRepository,
                          ProductoInsumoRepository productoInsumoRepository,
                          ComboProductoRepository comboProductoRepository,
                          PromocionRepository promocionRepository,
                          PromocionProductoRepository promocionProductoRepository,
                          DetallePedidoRepository detallePedidoRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoInsumoRepository = productoInsumoRepository;
        this.comboProductoRepository = comboProductoRepository;
        this.promocionRepository = promocionRepository;
        this.promocionProductoRepository = promocionProductoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    public List<Categoria> listCategorias() {
        return categoriaRepository.findAll();
    }

    public List<Producto> listProductos() {
        return productoRepository.findAll();
    }

    public List<Producto> listProductosDisponibles() {
        return productoRepository.findByDisponibleTrue();
    }

    public List<Producto> listProductosDisponiblesPorCategoria(Integer idCategoria) {
        return productoRepository.findByDisponibleTrueAndCategoria_IdCategoria(idCategoria);
    }

    public List<ProductoDTO> listProductosDtoPorCategoria(Integer idCategoria) {
        return listProductosDisponiblesPorCategoria(idCategoria).stream()
                .map(ProductoDTO::from)
                .toList();
    }

    public Producto getProducto(Integer idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", idProducto));
    }

    public Categoria getCategoria(Integer idCategoria) {
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", idCategoria));
    }

    public List<ProductoInsumo> getReceta(Integer idProducto) {
        return productoInsumoRepository.findByProducto_IdProducto(idProducto);
    }

    public List<ComboProducto> getComboItems(Integer idCombo) {
        return comboProductoRepository.findByCombo_IdProducto(idCombo);
    }

    public List<Promocion> listPromociones() {
        return promocionRepository.findAll();
    }

    public boolean codigoEnUso(String codigo, Integer idExcluir) {
        return idExcluir == null
                ? productoRepository.existsByCodigoIgnoreCase(codigo)
                : productoRepository.existsByCodigoIgnoreCaseAndIdProductoNot(codigo, idExcluir);
    }

    /**
     * Genera el siguiente codigo de producto (prefijo PZ) de forma secuencial.
     * El codigo es asignado por el backend; el cliente nunca lo define.
     */
    public String generarCodigoProducto() {
        String ultimo = productoRepository.findTopByCodigoStartingWithOrderByCodigoDesc("PZ")
                .map(Producto::getCodigo).orElse(null);
        return CodigoUtil.siguiente("PZ", ultimo);
    }

    @Transactional
    public Producto crearProducto(ProductoFormDTO form) {
        Producto producto = new Producto();
        producto.setCodigo(generarCodigoProducto());
        return guardarProducto(producto, form);
    }

    @Transactional
    public Producto actualizarProducto(Integer idProducto, ProductoFormDTO form) {
        // El codigo es inmutable: se conserva el del producto existente.
        return guardarProducto(getProducto(idProducto), form);
    }

    private Producto guardarProducto(Producto producto, ProductoFormDTO form) {
        producto.setNombre(form.getNombre().trim());
        producto.setPrecio(form.getPrecio());
        producto.setStock(form.getStock());
        producto.setTamanio(parseTamanio(form.getTamanio()));
        producto.setDisponible(form.isDisponible());
        producto.setCategoria(getCategoria(form.getIdCategoria()));
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto cambiarDisponibilidad(Integer idProducto, boolean disponible) {
        Producto producto = getProducto(idProducto);
        producto.setDisponible(disponible);
        return productoRepository.save(producto);
    }

    @Transactional
    public void eliminarProducto(Integer idProducto) {
        Producto producto = getProducto(idProducto);
        if (detallePedidoRepository.existsByProducto_IdProducto(idProducto)) {
            throw new IllegalStateException("'" + producto.getNombre()
                    + "' tiene ventas registradas. Desactivalo en su lugar.");
        }
        if (comboProductoRepository.existsByProducto_IdProducto(idProducto)) {
            throw new IllegalStateException("'" + producto.getNombre()
                    + "' forma parte de un combo. Quitalo del combo antes de eliminarlo.");
        }
        productoInsumoRepository.deleteAll(productoInsumoRepository.findByProducto_IdProducto(idProducto));
        comboProductoRepository.deleteAll(comboProductoRepository.findByCombo_IdProducto(idProducto));
        promocionProductoRepository.deleteAll(promocionProductoRepository.findByProducto_IdProducto(idProducto));
        productoRepository.delete(producto);
    }

    private Tamanio parseTamanio(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Tamanio.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ---- CRUD de promociones ----------------------------------------

    public Promocion getPromocion(Integer idPromocion) {
        return promocionRepository.findById(idPromocion)
                .orElseThrow(() -> ResourceNotFoundException.of("Promocion", idPromocion));
    }

    public List<Integer> productoIdsDePromocion(Integer idPromocion) {
        return promocionProductoRepository.findByPromocion_IdPromocion(idPromocion).stream()
                .map(pp -> pp.getProducto().getIdProducto())
                .toList();
    }

    @Transactional
    public Promocion crearPromocion(PromocionFormDTO form) {
        return guardarPromocion(new Promocion(), form);
    }

    @Transactional
    public Promocion actualizarPromocion(Integer idPromocion, PromocionFormDTO form) {
        return guardarPromocion(getPromocion(idPromocion), form);
    }

    private Promocion guardarPromocion(Promocion promocion, PromocionFormDTO form) {
        promocion.setNombre(form.getNombre().trim());
        promocion.setDescripcion(form.getDescripcion().trim());
        promocion.setTipoDescuento(form.getTipoDescuento());
        promocion.setValorDescuento(form.getValorDescuento());
        promocion.setActiva(form.isActiva());
        promocion = promocionRepository.save(promocion);

        promocionProductoRepository.deleteAll(
                promocionProductoRepository.findByPromocion_IdPromocion(promocion.getIdPromocion()));
        promocionProductoRepository.flush();
        for (Integer idProducto : form.getProductoIds()) {
            Producto producto = getProducto(idProducto);
            PromocionProducto vinculo = new PromocionProducto();
            vinculo.setId(new PromocionProductoId(promocion.getIdPromocion(), producto.getIdProducto()));
            vinculo.setPromocion(promocion);
            vinculo.setProducto(producto);
            vinculo.setCantidadMinima(1);
            promocionProductoRepository.save(vinculo);
        }
        return promocion;
    }

    @Transactional
    public Promocion cambiarPromocionActiva(Integer idPromocion, boolean activa) {
        Promocion promocion = getPromocion(idPromocion);
        promocion.setActiva(activa);
        return promocionRepository.save(promocion);
    }

    @Transactional
    public void eliminarPromocion(Integer idPromocion) {
        Promocion promocion = getPromocion(idPromocion);
        promocionProductoRepository.deleteAll(
                promocionProductoRepository.findByPromocion_IdPromocion(idPromocion));
        promocionRepository.delete(promocion);
    }

    public Promocion getPromocionActiva(Integer idProducto) {
        return promocionProductoRepository.findByProducto_IdProducto(idProducto).stream()
                .map(PromocionProducto::getPromocion)
                .filter(p -> Boolean.TRUE.equals(p.getActiva()))
                .findFirst()
                .orElse(null);
    }

    public BigDecimal calcularDescuento(Producto producto, int cantidad) {
        Promocion promo = getPromocionActiva(producto.getIdProducto());
        if (promo == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        BigDecimal descuento;
        if ("Porcentaje".equalsIgnoreCase(promo.getTipoDescuento())) {
            descuento = base.multiply(promo.getValorDescuento()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            descuento = promo.getValorDescuento().multiply(BigDecimal.valueOf(cantidad));
        }
        return descuento.setScale(2, RoundingMode.HALF_UP);
    }
}
