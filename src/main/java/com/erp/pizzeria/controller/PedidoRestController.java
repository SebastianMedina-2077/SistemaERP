package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.BoletaDTO;
import com.erp.pizzeria.dto.EstadoUpdateDTO;
import com.erp.pizzeria.dto.PedidoCocinaDTO;
import com.erp.pizzeria.dto.PedidoDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class PedidoRestController {

    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;

    public PedidoRestController(PedidoService pedidoService,
                                UsuarioRepository usuarioRepository) {
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/api/pedidos")
    public ResponseEntity<BoletaDTO> crearPedido(@Valid @RequestBody PedidoDTO dto,
                                                 Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        BoletaDTO boleta = pedidoService.crearPedido(dto, usuario.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(boleta);
    }

    @GetMapping("/api/pedidos/cocina")
    public List<PedidoCocinaDTO> cocina() {
        return pedidoService.getKitchenOrdersDTO();
    }

    @PatchMapping("/api/pedidos/{id}/estado")
    public Map<String, Object> actualizarEstado(@PathVariable Integer id,
                                                 @Valid @RequestBody EstadoUpdateDTO body) {
        EstadoPedido estado = parseEstado(body.getEstado());
        Pedido pedido = pedidoService.actualizarEstado(id, estado);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("idPedido", pedido.getIdPedido());
        resp.put("nuevoEstado", pedido.getEstado().name());
        return resp;
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado: " + authentication.getName()));
    }

    private EstadoPedido parseEstado(String valor) {
        try {
            return EstadoPedido.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de pedido invalido: " + valor);
        }
    }
}
