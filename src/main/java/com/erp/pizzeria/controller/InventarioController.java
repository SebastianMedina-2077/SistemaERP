package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.InsumoFormDTO;
import com.erp.pizzeria.dto.MovimientoFormDTO;
import com.erp.pizzeria.dto.MovimientoLineaDTO;
import com.erp.pizzeria.model.DetalleMovimiento;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.Movimiento;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.service.CompraService;
import com.erp.pizzeria.service.InventarioService;
import com.erp.pizzeria.util.PageQuery;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class InventarioController {

    private final InventarioService inventarioService;
    private final CompraService compraService;
    private final UsuarioRepository usuarioRepository;

    public InventarioController(InventarioService inventarioService,
                                CompraService compraService,
                                UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.compraService = compraService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/insumos")
    public String insumos(Model model) {
        model.addAttribute("active", "insumos");
        model.addAttribute("pageTitle", "Insumos");
        model.addAttribute("insumos", inventarioService.listInsumos());
        return "admin/insumos";
    }

    @GetMapping("/insumos/nuevo")
    public String nuevoInsumo(Model model) {
        prepararFormularioInsumo(model, null);
        if (!model.containsAttribute("insumoForm")) {
            InsumoFormDTO form = new InsumoFormDTO();
            form.setCodigo(inventarioService.generarCodigoInsumo());
            model.addAttribute("insumoForm", form);
        }
        return "admin/insumo-form";
    }

    @PostMapping("/insumos")
    public String crearInsumo(@Valid @ModelAttribute("insumoForm") InsumoFormDTO form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes ra) {
        // El codigo lo asigna el backend (InventarioService); no se valida unicidad del cliente.
        if (result.hasErrors()) {
            prepararFormularioInsumo(model, null);
            return "admin/insumo-form";
        }
        inventarioService.crearInsumo(form);
        ra.addFlashAttribute("flash", "Insumo '" + form.getNombre() + "' registrado.");
        return "redirect:/admin/insumos";
    }

    @GetMapping("/insumos/{id}/editar")
    public String editarInsumo(@PathVariable Integer id, Model model) {
        Insumo insumo = inventarioService.getInsumo(id);
        prepararFormularioInsumo(model, id);
        if (!model.containsAttribute("insumoForm")) {
            model.addAttribute("insumoForm", InsumoFormDTO.from(insumo));
        }
        return "admin/insumo-form";
    }

    @PostMapping("/insumos/{id}")
    public String actualizarInsumo(@PathVariable Integer id,
                                   @Valid @ModelAttribute("insumoForm") InsumoFormDTO form,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes ra) {
        // El codigo es inmutable en edicion; no se valida ni se cambia.
        if (result.hasErrors()) {
            prepararFormularioInsumo(model, id);
            return "admin/insumo-form";
        }
        inventarioService.actualizarInsumo(id, form);
        ra.addFlashAttribute("flash", "Insumo '" + form.getNombre() + "' actualizado.");
        return "redirect:/admin/insumos";
    }

    @PostMapping("/insumos/{id}/eliminar")
    public String eliminarInsumo(@PathVariable Integer id, RedirectAttributes ra) {
        Insumo insumo = inventarioService.getInsumo(id);
        try {
            inventarioService.eliminarInsumo(id);
            ra.addFlashAttribute("flash", "Insumo '" + insumo.getNombre() + "' eliminado.");
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            ra.addFlashAttribute("flashError", ex instanceof IllegalStateException ? ex.getMessage()
                    : "'" + insumo.getNombre() + "' tiene registros asociados y no puede eliminarse.");
        }
        return "redirect:/admin/insumos";
    }

    private void prepararFormularioInsumo(Model model, Integer editId) {
        model.addAttribute("active", "insumos");
        model.addAttribute("pageTitle", editId == null ? "Nuevo insumo" : "Editar insumo");
        model.addAttribute("medidas", inventarioService.listMedidas());
        model.addAttribute("editId", editId);
    }

    @GetMapping("/compras")
    public String compras(Model model) {
        model.addAttribute("active", "compras");
        model.addAttribute("pageTitle", "Compras");
        model.addAttribute("compras", compraService.listCompras());
        return "admin/compras";
    }

    @GetMapping("/movimientos")
    public String movimientos(@RequestParam(required = false) Integer tipo,
                              @RequestParam(required = false) String q,
                              @PageableDefault(size = 10, sort = "idMovimiento", direction = Sort.Direction.DESC) Pageable pageable,
                              Model model) {
        String qFiltro = (q == null || q.isBlank()) ? null : q.trim();
        Page<Movimiento> page = inventarioService.buscarMovimientos(tipo, qFiltro, pageable);

        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("tipo", tipo);
        filtros.put("q", qFiltro);

        model.addAttribute("active", "movimientos");
        model.addAttribute("pageTitle", "Movimientos");
        model.addAttribute("movimientos", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("tipos", inventarioService.listTiposMovimiento());
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("filtroQ", qFiltro != null ? qFiltro : "");
        model.addAttribute("baseUrl", "/admin/movimientos");
        model.addAttribute("query", PageQuery.of(filtros));
        return "admin/movimientos";
    }

    @GetMapping("/movimientos/nuevo")
    public String nuevoMovimiento(Model model) {
        prepararFormularioMovimiento(model);
        if (!model.containsAttribute("movimientoForm")) {
            MovimientoFormDTO form = new MovimientoFormDTO();
            form.getLineas().add(new MovimientoLineaDTO());
            model.addAttribute("movimientoForm", form);
        }
        return "admin/movimiento-form";
    }

    @PostMapping("/movimientos")
    public String registrarMovimiento(@Valid @ModelAttribute("movimientoForm") MovimientoFormDTO form,
                                      BindingResult result,
                                      Authentication authentication,
                                      Model model,
                                      RedirectAttributes ra) {
        if (result.hasErrors()) {
            asegurarLineaMovimiento(form);
            prepararFormularioMovimiento(model);
            return "admin/movimiento-form";
        }
        try {
            if (authentication == null) {
                throw new IllegalStateException("No hay usuario autenticado.");
            }
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado."));
            Movimiento movimiento = inventarioService.registrarMovimientoManual(form, usuario);
            ra.addFlashAttribute("flash", "Movimiento #" + movimiento.getIdMovimiento() + " registrado.");
            return "redirect:/admin/movimientos";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            asegurarLineaMovimiento(form);
            prepararFormularioMovimiento(model);
            model.addAttribute("flashError", ex.getMessage());
            return "admin/movimiento-form";
        }
    }

    private void prepararFormularioMovimiento(Model model) {
        model.addAttribute("active", "movimientos");
        model.addAttribute("pageTitle", "Nuevo movimiento");
        model.addAttribute("tipos", inventarioService.listTiposMovimientoManual());
        model.addAttribute("insumos", inventarioService.listInsumos());
    }

    private void asegurarLineaMovimiento(MovimientoFormDTO form) {
        if (form.getLineas() == null || form.getLineas().isEmpty()) {
            form.setLineas(new ArrayList<>());
            form.getLineas().add(new MovimientoLineaDTO());
        }
    }

    @GetMapping("/kardex")
    public String kardex(@RequestParam(required = false) String q,
                         @PageableDefault(size = 10, sort = "idDetalleMovimiento", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        String qFiltro = (q == null || q.isBlank()) ? null : q.trim();
        Page<DetalleMovimiento> page = inventarioService.buscarKardex(qFiltro, pageable);

        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("q", qFiltro);

        model.addAttribute("active", "kardex");
        model.addAttribute("pageTitle", "Kardex de insumos");
        model.addAttribute("kardex", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("filtroQ", qFiltro != null ? qFiltro : "");
        model.addAttribute("baseUrl", "/admin/kardex");
        model.addAttribute("query", PageQuery.of(filtros));
        return "admin/kardex";
    }
}
