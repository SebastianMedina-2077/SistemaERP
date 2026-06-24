package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.InsumoFormDTO;
import com.erp.pizzeria.dto.MovimientoComprobanteDTO;
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
import com.erp.pizzeria.audit.Audit;
import com.erp.pizzeria.dto.MovimientoFormDTO;
import com.erp.pizzeria.dto.MovimientoLineaDTO;
import com.erp.pizzeria.repository.DetalleCompraRepository;
import com.erp.pizzeria.repository.DetalleMovimientoRepository;
import com.erp.pizzeria.repository.InsumoRepository;
import com.erp.pizzeria.repository.MedidaRepository;
import com.erp.pizzeria.repository.MovimientoRepository;
import com.erp.pizzeria.repository.ProductoInsumoRepository;
import com.erp.pizzeria.repository.TipoMovimientoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private static final BigDecimal STOCK_INICIAL = BigDecimal.ZERO;
    private static final List<String> TIPOS_AUTOMATICOS = List.of("Compra", "Venta");

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

    public List<TipoMovimiento> listTiposMovimiento() {
        return tipoMovimientoRepository.findAll();
    }

    public List<TipoMovimiento> listTiposMovimientoManual() {
        return tipoMovimientoRepository.findAll().stream()
                .filter(t -> !TIPOS_AUTOMATICOS.contains(t.getDescripcion()))
                .toList();
    }

    public Page<Movimiento> buscarMovimientos(Integer idTipo, String q, Pageable pageable) {
        return movimientoRepository.buscar(idTipo, q, pageable);
    }

    public MovimientoComprobanteDTO getComprobanteMovimiento(Integer idMovimiento) {
        Movimiento movimiento = movimientoRepository.findComprobanteById(idMovimiento)
                .orElseThrow(() -> ResourceNotFoundException.of("Movimiento", idMovimiento));
        List<DetalleMovimiento> detalles = detalleMovimientoRepository.findComprobanteLineas(idMovimiento);
        return MovimientoComprobanteDTO.from(movimiento, detalles);
    }

    public Page<DetalleMovimiento> buscarKardex(String q, Pageable pageable) {
        return detalleMovimientoRepository.buscar(q, pageable);
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

    @Audit(accion = "CREAR", entidad = "Insumo")
    @Transactional
    public Insumo crearInsumo(InsumoFormDTO form) {
        Insumo insumo = new Insumo();
        insumo.setCodigo(generarCodigoInsumo());
        return guardarInsumo(insumo, form);
    }

    @Audit(accion = "EDITAR", entidad = "Insumo")
    @Transactional
    public Insumo actualizarInsumo(Integer idInsumo, InsumoFormDTO form) {
        // El codigo es inmutable: se conserva el del insumo existente.
        return guardarInsumo(getInsumo(idInsumo), form);
    }

    private Insumo guardarInsumo(Insumo insumo, InsumoFormDTO form) {
        boolean nuevo = insumo.getIdInsumo() == null;
        insumo.setNombre(form.getNombre().trim());
        insumo.setPrecio(form.getPrecio());
        if (nuevo) {
            insumo.setStock(STOCK_INICIAL);
        }
        insumo.setCantidadMinima(form.getCantidadMinima());
        insumo.setMedida(medidaRepository.findById(form.getIdMedida())
                .orElseThrow(() -> ResourceNotFoundException.of("Medida", form.getIdMedida())));
        insumo.setEstado(insumo.getStock().compareTo(form.getCantidadMinima()) <= 0
                ? EstadoInsumo.bajo : EstadoInsumo.normal);
        return insumoRepository.save(insumo);
    }

    @Audit(accion = "ELIMINAR", entidad = "Insumo")
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

    @Audit(accion = "REGISTRAR", entidad = "Movimiento")
    @Transactional
    public Movimiento registrarMovimientoManual(MovimientoFormDTO form, Usuario usuario) {
        TipoMovimiento tipo = tipoMovimientoRepository.findById(form.getIdTipoMovimiento())
                .orElseThrow(() -> ResourceNotFoundException.of("TipoMovimiento", form.getIdTipoMovimiento()));
        if (TIPOS_AUTOMATICOS.contains(tipo.getDescripcion())) {
            throw new IllegalArgumentException("Las compras y ventas se registran desde sus modulos propios.");
        }

        Map<Insumo, BigDecimal> lineas = new LinkedHashMap<>();
        for (MovimientoLineaDTO linea : form.getLineas()) {
            Insumo insumo = getInsumo(linea.getIdInsumo());
            lineas.merge(insumo, linea.getCantidad(), BigDecimal::add);
        }
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("Agregue al menos un insumo al movimiento.");
        }

        String documento = normalizar(form.getDocumento());
        String glosa = normalizar(form.getGlosa());
        return aplicarMovimiento(tipo.getDescripcion(), documento, glosa, usuario, null, lineas);
    }

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
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor a cero.");
            }
            BigDecimal resultante = salida ? insumo.getStock().subtract(cantidad) : insumo.getStock().add(cantidad);
            if (resultante.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Stock insuficiente para '" + insumo.getNombre() + "'.");
            }
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

    private String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
