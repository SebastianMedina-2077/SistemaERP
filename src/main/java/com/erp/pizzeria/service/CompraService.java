package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.CompraDTO;
import com.erp.pizzeria.dto.CompraLineaDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Compra;
import com.erp.pizzeria.model.DetalleCompra;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.Proveedor;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.CompraRepository;
import com.erp.pizzeria.repository.DetalleCompraRepository;
import com.erp.pizzeria.repository.InsumoRepository;
import com.erp.pizzeria.repository.ProveedorRepository;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.audit.Audit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CompraService {

    private static final String ESTADO_REGISTRADA = "Registrada";
    private static final String TIPO_COMPRA = "Compra";

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final InsumoRepository insumoRepository;
    private final InventarioService inventarioService;

    public CompraService(CompraRepository compraRepository,
                         DetalleCompraRepository detalleCompraRepository,
                         ProveedorRepository proveedorRepository,
                         UsuarioRepository usuarioRepository,
                         InsumoRepository insumoRepository,
                         InventarioService inventarioService) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.insumoRepository = insumoRepository;
        this.inventarioService = inventarioService;
    }

    public List<Compra> listCompras() {
        return compraRepository.findAll();
    }

    @Audit(accion = "REGISTRAR", entidad = "Compra")
    @Transactional
    public Compra registrarCompra(CompraDTO dto, Integer idUsuario) {
        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", dto.getIdProveedor()));
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", idUsuario));

        BigDecimal total = BigDecimal.ZERO;
        for (CompraLineaDTO linea : dto.getItems()) {
            total = total.add(linea.getCantidad().multiply(linea.getPrecioUnitario()));
        }

        Compra compra = new Compra();
        compra.setFecha(LocalDate.now());
        compra.setTotal(total);
        compra.setEstado(ESTADO_REGISTRADA);
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra = compraRepository.save(compra);

        Map<Insumo, BigDecimal> ingreso = new LinkedHashMap<>();
        for (CompraLineaDTO linea : dto.getItems()) {
            Insumo insumo = insumoRepository.findById(linea.getIdInsumo())
                    .orElseThrow(() -> ResourceNotFoundException.of("Insumo", linea.getIdInsumo()));

            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(compra);
            detalle.setInsumo(insumo);
            detalle.setCantidad(linea.getCantidad());
            detalle.setPrecioUnitario(linea.getPrecioUnitario());
            detalle.setSubtotal(linea.getCantidad().multiply(linea.getPrecioUnitario()));
            detalleCompraRepository.save(detalle);

            ingreso.merge(insumo, linea.getCantidad(), BigDecimal::add);
        }

        String documento = String.format("C-%04d", compra.getIdCompra());
        inventarioService.aplicarMovimiento(TIPO_COMPRA, documento, "Ingreso por compra", usuario, compra, ingreso);

        return compra;
    }
}
