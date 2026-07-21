package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.CotizacionRequestDTO;
import com.erp.pizzeria.dto.TiendaCatalogoDTO;
import com.erp.pizzeria.dto.TiendaCotizacionDTO;
import com.erp.pizzeria.dto.TiendaPedidoCreadoDTO;
import com.erp.pizzeria.dto.TiendaPedidoDTO;
import com.erp.pizzeria.dto.TiendaPedidoResumenDTO;
import com.erp.pizzeria.dto.TiendaPromoValidacionRequestDTO;
import com.erp.pizzeria.dto.TiendaPromoValidacionResponseDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.exception.StockInsuficienteException;
import com.erp.pizzeria.service.TiendaPedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API de la tienda en linea (cadena stateless de /api/tienda/**, ver SecurityConfig):
 * catalogo y cotizacion son publicos; crear pedidos exige sesion JWT (rol CLIENTE_WEB).
 * Los errores salen como JSON {error: ...} en el formato que consume static/js/tienda.js.
 */
@RestController
@RequestMapping("/api/tienda")
public class TiendaRestController {

    private final TiendaPedidoService tiendaPedidoService;

    public TiendaRestController(TiendaPedidoService tiendaPedidoService) {
        this.tiendaPedidoService = tiendaPedidoService;
    }

    @GetMapping("/catalogo")
    public TiendaCatalogoDTO catalogo() {
        return tiendaPedidoService.catalogo();
    }

    /** Detalle de un producto con sus adicionales (para la pagina de personalizacion). Publico. */
    @GetMapping("/producto/{id}")
    public com.erp.pizzeria.dto.TiendaProductoDetalleDTO producto(
            @org.springframework.web.bind.annotation.PathVariable Integer id) {
        return tiendaPedidoService.detalleProducto(id);
    }

    /**
     * Total autoritativo con promociones: misma cotizacion que usa el POS.
     * Acepta un codigo de cupon opcional; si es valido, el descuento ya viene
     * reflejado en lineas/total y la respuesta lo marca con cuponAplicado.
     */
    @PostMapping("/cotizar")
    public TiendaCotizacionDTO cotizar(@Valid @RequestBody CotizacionRequestDTO body) {
        return tiendaPedidoService.cotizar(body.getItems(), body.getCodigo());
    }

    /**
     * Validacion informativa de un codigo de cupon para el checkout: solo confirma
     * si existe y esta activo. El descuento real lo aplica /api/tienda/cotizar
     * cuando recibe el codigo (total autoritativo).
     */
    @PostMapping("/promo/validar")
    public ResponseEntity<TiendaPromoValidacionResponseDTO> validarPromo(@Valid @RequestBody TiendaPromoValidacionRequestDTO body) {
        TiendaPromoValidacionResponseDTO resultado = tiendaPedidoService.validarPromo(body.getCodigo());
        return resultado.isValido()
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(resultado);
    }

    @PostMapping("/pedidos")
    public ResponseEntity<TiendaPedidoCreadoDTO> crearPedido(@Valid @RequestBody TiendaPedidoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaPedidoService.crearPedido(dto));
    }

    /** Historial de la cuenta autenticada (JWT), del mas reciente al mas antiguo. */
    @GetMapping("/pedidos")
    public List<TiendaPedidoResumenDTO> misPedidos() {
        return tiendaPedidoService.misPedidos();
    }

    // ---- Errores en el formato JSON de la tienda ({error: ...}) ----

    @ExceptionHandler(TiendaPedidoService.PagosNoCuadranException.class)
    public ResponseEntity<Map<String, Object>> pagosNoCuadran(TiendaPedidoService.PagosNoCuadranException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("total", ex.getTotal());
        body.put("pagado", ex.getPagado());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(TiendaPedidoService.TarjetaNoAutorizadaException.class)
    public ResponseEntity<Map<String, String>> tarjetaNoAutorizada(TiendaPedidoService.TarjetaNoAutorizadaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> noAutenticado(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> sinStock(StockInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    /** Bean Validation: {error, campos: {campo: mensaje}} para pintar errores por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(nombreCampo(error.getField()), error.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Revisa los datos del pedido");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    /** Ultimo tramo de la ruta del campo: "entrega.direccion" -> "direccion". */
    private String nombreCampo(String ruta) {
        int punto = ruta.lastIndexOf('.');
        return punto >= 0 ? ruta.substring(punto + 1) : ruta;
    }
}
