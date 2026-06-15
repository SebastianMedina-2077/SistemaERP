package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.InsumoFormDTO;
import com.erp.pizzeria.dto.StockAlertDTO;
import com.erp.pizzeria.dto.StockFaltanteDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.exception.StockInsuficienteException;
import com.erp.pizzeria.model.Compra;
import com.erp.pizzeria.model.DetalleMovimiento;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.Medida;
import com.erp.pizzeria.model.Movimiento;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.model.ProductoInsumo;
import com.erp.pizzeria.model.TipoMovimiento;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.EstadoInsumo;
import com.erp.pizzeria.util.CodigoUtil;
import com.erp.pizzeria.repository.DetalleCompraRepository;
import com.erp.pizzeria.repository.DetalleMovimientoRepository;
import com.erp.pizzeria.repository.InsumoRepository;
import com.erp.pizzeria.repository.MedidaRepository;
import com.erp.pizzeria.repository.MovimientoRepository;
import com.erp.pizzeria.repository.ProductoInsumoRepository;
import com.erp.pizzeria.repository.TipoMovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InventarioService {

    private static final String OP_SALIDA = "Salida";

    private final InsumoRepository insumoRepository;
    private final MedidaRepository medidaRepository;
    private final ProductoInsumoRepository productoInsumoRepository;
    private final MovimientoRepository movimientoRepository;
    private final DetalleMovimientoRepository detalleMovimientoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final DetalleCompraRepository detalleCompraRepository;

    public InventarioService(InsumoRepository insumoRepository,
                             MedidaRepository medidaRepository,
                             ProductoInsumoRepository productoInsumoRepository,
                             MovimientoRepository movimientoRepository,
                             DetalleMovimientoRepository detalleMovimientoRepository,
                             TipoMovimientoRepository tipoMovimientoRepository,
                             DetalleCompraRepository detalleCompraRepository) {
        this.insumoRepository = insumoRepository;
        this.medidaRepository = medidaRepository;
        this.productoInsumoRepository = productoInsumoRepository;
        this.movimientoRepository = movimientoRepository;
        this.detalleMovimientoRepository = detalleMovimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.detalleCompraRepository = detalleCompraRepository;
    }

    // ---- Lecturas --------------------------------------------------

    public List<Medida> listMedidas() {
        return medidaRepository.findAll();
    }

    public List<Insumo> listInsumos() {
        return insumoRepository.findAll();
    }

    public Insumo getInsumo(Integer idInsumo) {
        return insumoRepository.findById(idInsumo)
                .orElseThrow(() -> ResourceNotFoundException.of("Insumo", idInsumo));
    }

    public List<Insumo> getInsumosBajoStock() {
        return insumoRepository.findAll().stream()
                .filter(i -> i.getStock().compareTo(i.getCantidadMinima()) <= 0)
                .toList();
    }

    public List<DetalleMovimiento> getKardex() {
        return detalleMovimientoRepository.findAll();
    }

    public List<Movimiento> listMovimientos() {
        return movimientoRepository.findAll();
    }

    // ---- CRUD de insumos --------------------------------------------

    public boolean codigoInsumoEnUso(String codigo, Integer idExcluir) {
        return idExcluir == null
                ? insumoRepository.existsByCodigoIgnoreCase(codigo)
                : insumoRepository.existsByCodigoIgnoreCaseAndIdInsumoNot(codigo, idExcluir);
    }

    /**
     * Genera el siguiente codigo de insumo (prefijo IN) de forma secuencial.
     * El codigo es asignado por el backend; el cliente nunca lo define.
     */
    public String generarCodigoInsumo() {
        String ultimo = insumoRepository.findTopByCodigoStartingWithOrderByCodigoDesc("IN")
                .map(Insumo::getCodigo).orElse(null);
        return CodigoUtil.siguiente("IN", ultimo);
    }

    @Transactional
    public Insumo crearInsumo(InsumoFormDTO form) {
        Insumo insumo = new Insumo();
        insumo.setCodigo(generarCodigoInsumo());
        return guardarInsumo(insumo, form);
    }

    @Transactional
    public Insumo actualizarInsumo(Integer idInsumo, InsumoFormDTO form) {
        // El codigo es inmutable: se conserva el del insumo existente.
        return guardarInsumo(getInsumo(idInsumo), form);
    }

    private Insumo guardarInsumo(Insumo insumo, InsumoFormDTO form) {
        insumo.setNombre(form.getNombre().trim());
        insumo.setPrecio(form.getPrecio());
        insumo.setStock(form.getStock());
        insumo.setCantidadMinima(form.getCantidadMinima());
        insumo.setMedida(medidaRepository.findById(form.getIdMedida())
                .orElseThrow(() -> ResourceNotFoundException.of("Medida", form.getIdMedida())));
        insumo.setEstado(form.getStock().compareTo(form.getCantidadMinima()) <= 0
                ? EstadoInsumo.bajo : EstadoInsumo.normal);
        return insumoRepository.save(insumo);
    }

    @Transactional
    public void eliminarInsumo(Integer idInsumo) {
        Insumo insumo = getInsumo(idInsumo);
        if (productoInsumoRepository.existsByInsumo_IdInsumo(idInsumo)) {
            throw new IllegalStateException("'" + insumo.getNombre()
                    + "' es parte de la receta de un producto. Quitalo de las recetas primero.");
        }
        if (detalleMovimientoRepository.existsByInsumo_IdInsumo(idInsumo)
                || detalleCompraRepository.existsByInsumo_IdInsumo(idInsumo)) {
            throw new IllegalStateException("'" + insumo.getNombre()
                    + "' tiene movimientos o compras en el kardex y no puede eliminarse.");
        }
        insumoRepository.delete(insumo);
    }

    // ---- Calculo de consumo de insumos -----------------------------

    public Map<Insumo, BigDecimal> consumoDeProducto(Producto producto, int cantidad) {
        Map<Insumo, BigDecimal> consumo = new LinkedHashMap<>();
        for (ProductoInsumo pi : productoInsumoRepository.findByProducto_IdProducto(producto.getIdProducto())) {
            BigDecimal requerido = pi.getCantidad().multiply(BigDecimal.valueOf(cantidad));
            consumo.merge(pi.getInsumo(), requerido, BigDecimal::add);
        }
        return consumo;
    }

    public Map<Insumo, BigDecimal> combinar(Map<Insumo, BigDecimal> acumulado, Map<Insumo, BigDecimal> nuevo) {
        nuevo.forEach((insumo, cant) -> acumulado.merge(insumo, cant, BigDecimal::add));
        return acumulado;
    }

    // ---- Verificacion ---------------------------------------------

    public StockAlertDTO verificarStock(Producto producto, int cantidad) {
        List<StockFaltanteDTO> faltantes = faltantesDe(consumoDeProducto(producto, cantidad));
        return new StockAlertDTO(faltantes.isEmpty(), faltantes);
    }

    public void verificarDisponibilidad(Map<Insumo, BigDecimal> consumo) {
        List<StockFaltanteDTO> faltantes = faltantesDe(consumo);
        if (!faltantes.isEmpty()) {
            String nombres = faltantes.stream().map(StockFaltanteDTO::getInsumo).reduce((a, b) -> a + ", " + b).orElse("");
            throw new StockInsuficienteException("Stock insuficiente: " + nombres, faltantes);
        }
    }

    private List<StockFaltanteDTO> faltantesDe(Map<Insumo, BigDecimal> consumo) {
        List<StockFaltanteDTO> faltantes = new ArrayList<>();
        consumo.forEach((insumo, requerido) -> {
            if (insumo.getStock().compareTo(requerido) < 0) {
                faltantes.add(new StockFaltanteDTO(insumo.getNombre(), requerido, insumo.getStock()));
            }
        });
        return faltantes;
    }

    // ---- Aplicacion de movimientos (escritura) ---------------------

    @Transactional
    public Movimiento aplicarMovimiento(String tipoDescripcion, String documento, String glosa,
                                        Usuario usuario, Compra compra, Map<Insumo, BigDecimal> lineas) {
        TipoMovimiento tipo = tipoMovimientoRepository.findByDescripcion(tipoDescripcion)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de movimiento no encontrado: " + tipoDescripcion));
        boolean salida = OP_SALIDA.equalsIgnoreCase(tipo.getOperacion());

        Movimiento movimiento = new Movimiento();
        movimiento.setDocumento(documento);
        movimiento.setFecha(LocalDate.now());
        movimiento.setGlosa(glosa);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCompra(compra);
        movimiento.setUsuario(usuario);
        movimiento = movimientoRepository.save(movimiento);

        for (Map.Entry<Insumo, BigDecimal> linea : lineas.entrySet()) {
            Insumo insumo = linea.getKey();
            BigDecimal cantidad = linea.getValue();
            BigDecimal resultante = salida ? insumo.getStock().subtract(cantidad) : insumo.getStock().add(cantidad);
            insumo.setStock(resultante);
            insumo.setEstado(resultante.compareTo(insumo.getCantidadMinima()) <= 0 ? EstadoInsumo.bajo : EstadoInsumo.normal);
            insumoRepository.save(insumo);

            DetalleMovimiento detalle = new DetalleMovimiento();
            detalle.setMovimiento(movimiento);
            detalle.setInsumo(insumo);
            detalle.setCantidad(cantidad);
            detalle.setStockResultante(resultante);
            detalleMovimientoRepository.save(detalle);
        }
        return movimiento;
    }
}
