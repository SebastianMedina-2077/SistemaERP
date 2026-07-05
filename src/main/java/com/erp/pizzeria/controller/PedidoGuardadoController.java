package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.PedidoGuardadoDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.service.PedidoGuardadoStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class PedidoGuardadoController {

    private final PedidoGuardadoStore store;
    private final UsuarioRepository usuarioRepository;

    public PedidoGuardadoController(PedidoGuardadoStore store, UsuarioRepository usuarioRepository) {
        this.store = store;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/cajero/guardados")
    public String vista(Model model, Authentication auth) {
        model.addAttribute("pedidosGuardados", store.listar(cajeroId(auth)));
        return "ventas/guardados";
    }

    @PostMapping("/cajero/pedidos/guardar")
    @ResponseBody
    public PedidoGuardadoDTO guardar(@RequestBody PedidoGuardadoDTO dto, Authentication auth) {
        return store.guardar(cajeroId(auth), dto);
    }

    @PostMapping("/cajero/pedidos/guardados/{id}/recuperar")
    @ResponseBody
    public PedidoGuardadoDTO recuperar(@PathVariable Long id, Authentication auth) {
        return store.recuperar(cajeroId(auth), id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido guardado no encontrado: " + id));
    }

    @DeleteMapping("/cajero/pedidos/guardados/{id}")
    @ResponseBody
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        return store.eliminar(cajeroId(auth), id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/cajero/pedidos/guardados/count")
    @ResponseBody
    public Map<String, Integer> contar(Authentication auth) {
        return Map.of("count", store.contar(cajeroId(auth)));
    }

    private Integer cajeroId(Authentication auth) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado: " + auth.getName()));
        return usuario.getIdUsuario();
    }
}
