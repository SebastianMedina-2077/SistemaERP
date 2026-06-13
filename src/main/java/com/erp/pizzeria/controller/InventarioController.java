package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.InsumoFormDTO;
import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.service.CompraService;
import com.erp.pizzeria.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class InventarioController {

    private final InventarioService inventarioService;
    private final CompraService compraService;

    public InventarioController(InventarioService inventarioService, CompraService compraService) {
        this.inventarioService = inventarioService;
        this.compraService = compraService;
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
            model.addAttribute("insumoForm", new InsumoFormDTO());
        }
        return "admin/insumo-form";
    }

    @PostMapping("/insumos")
    public String crearInsumo(@Valid @ModelAttribute("insumoForm") InsumoFormDTO form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes ra) {
        validarCodigoUnico(form, null, result);
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
        validarCodigoUnico(form, id, result);
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

    private void validarCodigoUnico(InsumoFormDTO form, Integer editId, BindingResult result) {
        if (!result.hasFieldErrors("codigo") && inventarioService.codigoInsumoEnUso(form.getCodigo(), editId)) {
            result.rejectValue("codigo", "duplicado", "Ya existe un insumo con este codigo");
        }
    }

    @GetMapping("/compras")
    public String compras(Model model) {
        model.addAttribute("active", "compras");
        model.addAttribute("pageTitle", "Compras");
        model.addAttribute("compras", compraService.listCompras());
        return "admin/compras";
    }

    @GetMapping("/movimientos")
    public String movimientos(Model model) {
        model.addAttribute("active", "movimientos");
        model.addAttribute("pageTitle", "Movimientos");
        model.addAttribute("movimientos", inventarioService.listMovimientos());
        return "admin/movimientos";
    }

    @GetMapping("/kardex")
    public String kardex(Model model) {
        model.addAttribute("active", "kardex");
        model.addAttribute("pageTitle", "Kardex de insumos");
        model.addAttribute("kardex", inventarioService.getKardex());
        return "admin/kardex";
    }
}
