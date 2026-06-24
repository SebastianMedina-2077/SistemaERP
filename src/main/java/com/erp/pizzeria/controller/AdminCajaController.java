package com.erp.pizzeria.controller;

import com.erp.pizzeria.service.CajaStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/caja")
public class AdminCajaController {

    private final CajaStore cajaStore;

    public AdminCajaController(CajaStore cajaStore) {
        this.cajaStore = cajaStore;
    }

    @GetMapping
    public String vista(Model model) {
        model.addAttribute("active", "caja");
        model.addAttribute("pageTitle", "Caja");
        model.addAttribute("bloqueadas", cajaStore.bloqueadas());
        model.addAttribute("supervisorPin", cajaStore.getSupervisorPin());
        return "admin/caja";
    }

    @PostMapping("/{cajeroId}/desbloquear")
    public String desbloquear(@PathVariable Integer cajeroId, RedirectAttributes ra) {
        boolean ok = cajaStore.desbloquearAdmin(cajeroId);
        ra.addFlashAttribute(ok ? "flash" : "flashError",
                ok ? "Caja desbloqueada correctamente." : "No se encontro la caja a desbloquear.");
        return "redirect:/admin/caja";
    }
}
