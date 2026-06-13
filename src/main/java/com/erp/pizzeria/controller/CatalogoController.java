package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.ProductoFormDTO;
import com.erp.pizzeria.dto.PromocionFormDTO;
import com.erp.pizzeria.model.Producto;
import com.erp.pizzeria.model.Promocion;
import com.erp.pizzeria.service.CatalogService;
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
public class CatalogoController {

    private final CatalogService catalogService;

    public CatalogoController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/productos")
    public String productos(Model model) {
        model.addAttribute("active", "productos");
        model.addAttribute("pageTitle", "Productos");
        model.addAttribute("productos", catalogService.listProductos());
        return "admin/productos";
    }

    @GetMapping("/productos/nuevo")
    public String nuevoProducto(Model model) {
        prepararFormulario(model, null);
        if (!model.containsAttribute("productoForm")) {
            model.addAttribute("productoForm", new ProductoFormDTO());
        }
        return "admin/producto-form";
    }

    @PostMapping("/productos")
    public String crearProducto(@Valid @ModelAttribute("productoForm") ProductoFormDTO form,
                                BindingResult result,
                                Model model,
                                RedirectAttributes ra) {
        validarCodigoUnico(form, null, result);
        if (result.hasErrors()) {
            prepararFormulario(model, null);
            return "admin/producto-form";
        }
        catalogService.crearProducto(form);
        ra.addFlashAttribute("flash", "Producto '" + form.getNombre() + "' registrado.");
        return "redirect:/admin/productos";
    }

    @GetMapping("/productos/{id}/editar")
    public String editarProducto(@PathVariable Integer id, Model model) {
        Producto producto = catalogService.getProducto(id);
        prepararFormulario(model, id);
        if (!model.containsAttribute("productoForm")) {
            model.addAttribute("productoForm", ProductoFormDTO.from(producto));
        }
        return "admin/producto-form";
    }

    @PostMapping("/productos/{id}")
    public String actualizarProducto(@PathVariable Integer id,
                                     @Valid @ModelAttribute("productoForm") ProductoFormDTO form,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes ra) {
        validarCodigoUnico(form, id, result);
        if (result.hasErrors()) {
            prepararFormulario(model, id);
            return "admin/producto-form";
        }
        catalogService.actualizarProducto(id, form);
        ra.addFlashAttribute("flash", "Producto '" + form.getNombre() + "' actualizado.");
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/{id}/disponibilidad")
    public String cambiarDisponibilidad(@PathVariable Integer id,
                                        RedirectAttributes ra) {
        Producto producto = catalogService.getProducto(id);
        boolean nuevoEstado = !Boolean.TRUE.equals(producto.getDisponible());
        catalogService.cambiarDisponibilidad(id, nuevoEstado);
        ra.addFlashAttribute("flash", "Producto '" + producto.getNombre() + "' "
                + (nuevoEstado ? "activado." : "desactivado."));
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/{id}/eliminar")
    public String eliminarProducto(@PathVariable Integer id, RedirectAttributes ra) {
        Producto producto = catalogService.getProducto(id);
        try {
            catalogService.eliminarProducto(id);
            ra.addFlashAttribute("flash", "Producto '" + producto.getNombre() + "' eliminado.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("flashError", "'" + producto.getNombre()
                    + "' tiene registros asociados y no puede eliminarse.");
        }
        return "redirect:/admin/productos";
    }

    // ---- Promociones -------------------------------------------------

    @GetMapping("/promociones")
    public String promociones(Model model) {
        model.addAttribute("active", "promociones");
        model.addAttribute("pageTitle", "Promociones");
        model.addAttribute("promociones", catalogService.listPromociones());
        return "admin/promociones";
    }

    @GetMapping("/promociones/nueva")
    public String nuevaPromocion(Model model) {
        prepararFormularioPromocion(model, null);
        if (!model.containsAttribute("promocionForm")) {
            model.addAttribute("promocionForm", new PromocionFormDTO());
        }
        return "admin/promocion-form";
    }

    @PostMapping("/promociones")
    public String crearPromocion(@Valid @ModelAttribute("promocionForm") PromocionFormDTO form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepararFormularioPromocion(model, null);
            return "admin/promocion-form";
        }
        catalogService.crearPromocion(form);
        ra.addFlashAttribute("flash", "Promocion '" + form.getNombre() + "' registrada.");
        return "redirect:/admin/promociones";
    }

    @GetMapping("/promociones/{id}/editar")
    public String editarPromocion(@PathVariable Integer id, Model model) {
        Promocion promocion = catalogService.getPromocion(id);
        prepararFormularioPromocion(model, id);
        if (!model.containsAttribute("promocionForm")) {
            model.addAttribute("promocionForm",
                    PromocionFormDTO.from(promocion, catalogService.productoIdsDePromocion(id)));
        }
        return "admin/promocion-form";
    }

    @PostMapping("/promociones/{id}")
    public String actualizarPromocion(@PathVariable Integer id,
                                      @Valid @ModelAttribute("promocionForm") PromocionFormDTO form,
                                      BindingResult result,
                                      Model model,
                                      RedirectAttributes ra) {
        if (result.hasErrors()) {
            prepararFormularioPromocion(model, id);
            return "admin/promocion-form";
        }
        catalogService.actualizarPromocion(id, form);
        ra.addFlashAttribute("flash", "Promocion '" + form.getNombre() + "' actualizada.");
        return "redirect:/admin/promociones";
    }

    @PostMapping("/promociones/{id}/activa")
    public String cambiarPromocionActiva(@PathVariable Integer id, RedirectAttributes ra) {
        Promocion promocion = catalogService.getPromocion(id);
        boolean nueva = !Boolean.TRUE.equals(promocion.getActiva());
        catalogService.cambiarPromocionActiva(id, nueva);
        ra.addFlashAttribute("flash", "Promocion '" + promocion.getNombre() + "' "
                + (nueva ? "activada." : "desactivada."));
        return "redirect:/admin/promociones";
    }

    @PostMapping("/promociones/{id}/eliminar")
    public String eliminarPromocion(@PathVariable Integer id, RedirectAttributes ra) {
        Promocion promocion = catalogService.getPromocion(id);
        catalogService.eliminarPromocion(id);
        ra.addFlashAttribute("flash", "Promocion '" + promocion.getNombre() + "' eliminada.");
        return "redirect:/admin/promociones";
    }

    private void prepararFormularioPromocion(Model model, Integer editId) {
        model.addAttribute("active", "promociones");
        model.addAttribute("pageTitle", editId == null ? "Nueva promocion" : "Editar promocion");
        model.addAttribute("productos", catalogService.listProductos());
        model.addAttribute("editId", editId);
    }

    private void prepararFormulario(Model model, Integer editId) {
        model.addAttribute("active", "productos");
        model.addAttribute("pageTitle", editId == null ? "Nuevo producto" : "Editar producto");
        model.addAttribute("categorias", catalogService.listCategorias());
        model.addAttribute("editId", editId);
    }

    private void validarCodigoUnico(ProductoFormDTO form, Integer editId, BindingResult result) {
        if (!result.hasFieldErrors("codigo") && catalogService.codigoEnUso(form.getCodigo(), editId)) {
            result.rejectValue("codigo", "duplicado", "Ya existe un producto con este codigo");
        }
    }
}
