package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.EmpleadoFormDTO;
import com.erp.pizzeria.dto.ProveedorFormDTO;
import com.erp.pizzeria.dto.UsuarioFormDTO;
import com.erp.pizzeria.model.Empleado;
import com.erp.pizzeria.model.Proveedor;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
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
public class PersonasController {

    private static final java.util.List<String> CARGOS =
            java.util.List.of("Administrador", "Cajero", "Cocina", "Mozo", "Repartidor");

    private final PersonaService personaService;

    public PersonasController(PersonaService personaService) {
        this.personaService = personaService;
    }

    // ---- Empleados -----------------------------------------------------

    @GetMapping("/empleados")
    public String empleados(Model model) {
        model.addAttribute("active", "empleados");
        model.addAttribute("pageTitle", "Empleados");
        model.addAttribute("empleados", personaService.listEmpleados());
        return "admin/empleados";
    }

    @GetMapping("/empleados/nuevo")
    public String nuevoEmpleado(Model model) {
        prepararFormularioEmpleado(model, null);
        if (!model.containsAttribute("empleadoForm")) {
            model.addAttribute("empleadoForm", new EmpleadoFormDTO());
        }
        return "admin/empleado-form";
    }

    @PostMapping("/empleados")
    public String crearEmpleado(@Valid @ModelAttribute("empleadoForm") EmpleadoFormDTO form,
                                BindingResult result,
                                Model model,
                                RedirectAttributes ra) {
        validarDniUnico(form, null, result);
        if (result.hasErrors()) {
            prepararFormularioEmpleado(model, null);
            return "admin/empleado-form";
        }
        personaService.crearEmpleado(form);
        ra.addFlashAttribute("flash", "Empleado '" + form.getNombre() + " " + form.getApellido() + "' registrado.");
        return "redirect:/admin/empleados";
    }

    @GetMapping("/empleados/{id}/editar")
    public String editarEmpleado(@PathVariable Integer id, Model model) {
        Empleado empleado = personaService.getEmpleado(id);
        prepararFormularioEmpleado(model, id);
        if (!model.containsAttribute("empleadoForm")) {
            model.addAttribute("empleadoForm", EmpleadoFormDTO.from(empleado));
        }
        return "admin/empleado-form";
    }

    @PostMapping("/empleados/{id}")
    public String actualizarEmpleado(@PathVariable Integer id,
                                     @Valid @ModelAttribute("empleadoForm") EmpleadoFormDTO form,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes ra) {
        validarDniUnico(form, id, result);
        if (result.hasErrors()) {
            prepararFormularioEmpleado(model, id);
            return "admin/empleado-form";
        }
        personaService.actualizarEmpleado(id, form);
        ra.addFlashAttribute("flash", "Empleado '" + form.getNombre() + " " + form.getApellido() + "' actualizado.");
        return "redirect:/admin/empleados";
    }

    @PostMapping("/empleados/{id}/eliminar")
    public String eliminarEmpleado(@PathVariable Integer id, RedirectAttributes ra) {
        Empleado empleado = personaService.getEmpleado(id);
        try {
            personaService.eliminarEmpleado(id);
            ra.addFlashAttribute("flash", "Empleado '" + empleado.getNombre() + " " + empleado.getApellido() + "' eliminado.");
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            ra.addFlashAttribute("flashError", ex instanceof IllegalStateException ? ex.getMessage()
                    : "El empleado tiene registros asociados y no puede eliminarse.");
        }
        return "redirect:/admin/empleados";
    }

    // ---- Usuarios ------------------------------------------------------

    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("active", "usuarios");
        model.addAttribute("pageTitle", "Usuarios");
        model.addAttribute("usuarios", personaService.listUsuarios());
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/nuevo")
    public String nuevoUsuario(Model model) {
        prepararFormularioUsuario(model, null);
        if (!model.containsAttribute("usuarioForm")) {
            model.addAttribute("usuarioForm", new UsuarioFormDTO());
        }
        return "admin/usuario-form";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@Valid @ModelAttribute("usuarioForm") UsuarioFormDTO form,
                               BindingResult result,
                               Model model,
                               RedirectAttributes ra) {
        validarUsername(form, null, result);
        validarPassword(form, true, result);
        if (result.hasErrors()) {
            prepararFormularioUsuario(model, null);
            return "admin/usuario-form";
        }
        personaService.crearUsuario(form);
        ra.addFlashAttribute("flash", "Usuario '" + form.getUsername() + "' registrado.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/{id}/editar")
    public String editarUsuario(@PathVariable Integer id, Model model) {
        Usuario usuario = personaService.getUsuario(id);
        prepararFormularioUsuario(model, id);
        if (!model.containsAttribute("usuarioForm")) {
            model.addAttribute("usuarioForm", UsuarioFormDTO.from(usuario));
        }
        return "admin/usuario-form";
    }

    @PostMapping("/usuarios/{id}")
    public String actualizarUsuario(@PathVariable Integer id,
                                    @Valid @ModelAttribute("usuarioForm") UsuarioFormDTO form,
                                    BindingResult result,
                                    Model model,
                                    RedirectAttributes ra) {
        validarUsername(form, id, result);
        validarPassword(form, false, result);
        if (result.hasErrors()) {
            prepararFormularioUsuario(model, id);
            return "admin/usuario-form";
        }
        personaService.actualizarUsuario(id, form);
        ra.addFlashAttribute("flash", "Usuario '" + form.getUsername() + "' actualizado.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstadoUsuario(@PathVariable Integer id,
                                       Authentication authentication,
                                       RedirectAttributes ra) {
        Usuario usuario = personaService.getUsuario(id);
        if (usuario.getUsername().equals(authentication.getName())) {
            ra.addFlashAttribute("flashError", "No puedes desactivar tu propia cuenta.");
            return "redirect:/admin/usuarios";
        }
        boolean nuevo = !Boolean.TRUE.equals(usuario.getEstado());
        personaService.cambiarEstadoUsuario(id, nuevo);
        ra.addFlashAttribute("flash", "Usuario '" + usuario.getUsername() + "' "
                + (nuevo ? "activado." : "desactivado."));
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable Integer id,
                                  Authentication authentication,
                                  RedirectAttributes ra) {
        Usuario usuario = personaService.getUsuario(id);
        if (usuario.getUsername().equals(authentication.getName())) {
            ra.addFlashAttribute("flashError", "No puedes eliminar tu propia cuenta.");
            return "redirect:/admin/usuarios";
        }
        try {
            personaService.eliminarUsuario(id);
            ra.addFlashAttribute("flash", "Usuario '" + usuario.getUsername() + "' eliminado.");
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            ra.addFlashAttribute("flashError", ex instanceof IllegalStateException ? ex.getMessage()
                    : "'" + usuario.getUsername() + "' tiene registros asociados y no puede eliminarse.");
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/proveedores")
    public String proveedores(Model model) {
        model.addAttribute("active", "proveedores");
        model.addAttribute("pageTitle", "Proveedores");
        model.addAttribute("proveedores", personaService.listProveedores());
        return "admin/proveedores";
    }

    @GetMapping("/proveedores/nuevo")
    public String nuevoProveedor(Model model) {
        prepararFormulario(model, null);
        if (!model.containsAttribute("proveedorForm")) {
            model.addAttribute("proveedorForm", new ProveedorFormDTO());
        }
        return "admin/proveedor-form";
    }

    @PostMapping("/proveedores")
    public String crearProveedor(@Valid @ModelAttribute("proveedorForm") ProveedorFormDTO form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes ra) {
        validarRucUnico(form, null, result);
        if (result.hasErrors()) {
            prepararFormulario(model, null);
            return "admin/proveedor-form";
        }
        personaService.crearProveedor(form);
        ra.addFlashAttribute("flash", "Proveedor '" + form.getNombre() + "' registrado.");
        return "redirect:/admin/proveedores";
    }

    @GetMapping("/proveedores/{id}/editar")
    public String editarProveedor(@PathVariable Integer id, Model model) {
        Proveedor proveedor = personaService.getProveedor(id);
        prepararFormulario(model, id);
        if (!model.containsAttribute("proveedorForm")) {
            model.addAttribute("proveedorForm", ProveedorFormDTO.from(proveedor));
        }
        return "admin/proveedor-form";
    }

    @PostMapping("/proveedores/{id}")
    public String actualizarProveedor(@PathVariable Integer id,
                                      @Valid @ModelAttribute("proveedorForm") ProveedorFormDTO form,
                                      BindingResult result,
                                      Model model,
                                      RedirectAttributes ra) {
        validarRucUnico(form, id, result);
        if (result.hasErrors()) {
            prepararFormulario(model, id);
            return "admin/proveedor-form";
        }
        personaService.actualizarProveedor(id, form);
        ra.addFlashAttribute("flash", "Proveedor '" + form.getNombre() + "' actualizado.");
        return "redirect:/admin/proveedores";
    }

    @PostMapping("/proveedores/{id}/eliminar")
    public String eliminarProveedor(@PathVariable Integer id, RedirectAttributes ra) {
        Proveedor proveedor = personaService.getProveedor(id);
        try {
            personaService.eliminarProveedor(id);
            ra.addFlashAttribute("flash", "Proveedor '" + proveedor.getNombre() + "' eliminado.");
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            ra.addFlashAttribute("flashError", "'" + proveedor.getNombre()
                    + "' tiene compras registradas y no puede eliminarse.");
        }
        return "redirect:/admin/proveedores";
    }

    private void prepararFormulario(Model model, Integer editId) {
        model.addAttribute("active", "proveedores");
        model.addAttribute("pageTitle", editId == null ? "Nuevo proveedor" : "Editar proveedor");
        model.addAttribute("editId", editId);
    }

    private void prepararFormularioEmpleado(Model model, Integer editId) {
        model.addAttribute("active", "empleados");
        model.addAttribute("pageTitle", editId == null ? "Nuevo empleado" : "Editar empleado");
        model.addAttribute("cargos", CARGOS);
        model.addAttribute("editId", editId);
    }

    private void prepararFormularioUsuario(Model model, Integer editId) {
        model.addAttribute("active", "usuarios");
        model.addAttribute("pageTitle", editId == null ? "Nuevo usuario" : "Editar usuario");
        model.addAttribute("roles", personaService.listRoles());
        model.addAttribute("empleados", personaService.listEmpleados());
        model.addAttribute("editId", editId);
    }

    private void validarDniUnico(EmpleadoFormDTO form, Integer editId, BindingResult result) {
        if (!result.hasFieldErrors("dni") && personaService.dniEnUso(form.getDni(), editId)) {
            result.rejectValue("dni", "duplicado", "Ya existe un empleado con este DNI");
        }
    }

    private void validarUsername(UsuarioFormDTO form, Integer editId, BindingResult result) {
        if (!result.hasFieldErrors("username") && personaService.usernameEnUso(form.getUsername(), editId)) {
            result.rejectValue("username", "duplicado", "Ya existe un usuario con este nombre");
        }
    }

    private void validarPassword(UsuarioFormDTO form, boolean obligatoria, BindingResult result) {
        String pwd = form.getPassword();
        if (obligatoria && (pwd == null || pwd.isBlank())) {
            result.rejectValue("password", "obligatoria", "La contrasena es obligatoria");
        } else if (pwd != null && !pwd.isBlank() && pwd.length() < 6) {
            result.rejectValue("password", "corta", "Minimo 6 caracteres");
        }
    }

    private void validarRucUnico(ProveedorFormDTO form, Integer editId, BindingResult result) {
        if (!result.hasFieldErrors("ruc") && personaService.rucEnUso(form.getRuc(), editId)) {
            result.rejectValue("ruc", "duplicado", "Ya existe un proveedor con este RUC");
        }
    }
}
