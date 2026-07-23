package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.CuentaTiendaDTO;
import com.erp.pizzeria.dto.LoginTiendaDTO;
import com.erp.pizzeria.dto.RegistroTiendaDTO;
import com.erp.pizzeria.dto.SesionTiendaDTO;
import com.erp.pizzeria.dto.TarjetaTiendaDTO;
import com.erp.pizzeria.model.Cliente;
import com.erp.pizzeria.model.ClienteCuenta;
import com.erp.pizzeria.repository.ClienteCuentaRepository;
import com.erp.pizzeria.repository.ClienteRepository;
import com.erp.pizzeria.repository.ClienteTarjetaRepository;
import com.erp.pizzeria.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Cuentas de clientes de la tienda web: registro, login y perfil.
 * La sesion es un JWT (ver JwtService/JwtAuthFilter), separada de la del personal.
 */
@Service
@Transactional(readOnly = true)
public class TiendaCuentaService {

    /** Email ya usado por otra cuenta (el controlador lo traduce a 409). */
    public static class EmailYaRegistradoException extends RuntimeException {
        public EmailYaRegistradoException(String mensaje) {
            super(mensaje);
        }
    }

    private final ClienteCuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteTarjetaRepository tarjetaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificacionCorreoService verificacionCorreoService;

    public TiendaCuentaService(ClienteCuentaRepository cuentaRepository,
                               ClienteRepository clienteRepository,
                               ClienteTarjetaRepository tarjetaRepository,
                               PasswordEncoder passwordEncoder,
                               JwtService jwtService,
                               VerificacionCorreoService verificacionCorreoService) {
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.tarjetaRepository = tarjetaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verificacionCorreoService = verificacionCorreoService;
    }

    /** Crea el Cliente y su cuenta web; devuelve la sesion con el JWT emitido. */
    @Transactional
    public SesionTiendaDTO registrar(RegistroTiendaDTO datos) {
        String email = normalizarEmail(datos.getEmail());
        if (cuentaRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException("El email ya esta registrado");
        }
        Cliente cliente = new Cliente();
        cliente.setNombre(datos.getNombres().trim());
        cliente.setApellidoPaterno(datos.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(datos.getApellidoMaterno().trim());
        cliente.setTelefono(datos.getTelefono());
        cliente.setDepartamento(datos.getDepartamento().trim());
        cliente.setProvincia(datos.getProvincia().trim());
        cliente.setDistrito(datos.getDistrito().trim());
        cliente.setDireccionExacta(datos.getDireccionExacta().trim());
        cliente.setReferencia(datos.getReferencia() == null ? null : datos.getReferencia().trim());
        clienteRepository.save(cliente);

        ClienteCuenta cuenta = new ClienteCuenta();
        cuenta.setCliente(cliente);
        cuenta.setEmail(email);
        cuenta.setPasswordHash(passwordEncoder.encode(datos.getPassword()));
        cuenta.setActivo(true);
        cuenta.setEmailVerificado(false);
        cuenta.setFechaRegistro(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        // Verificacion blanda: se envia el codigo pero no bloquea el login ni el JWT.
        verificacionCorreoService.generarYEnviar(email);

        return emitirSesion(cuenta);
    }

    /**
     * Autentica por email y contrasena. No distingue en la respuesta si el email
     * existe o no (evita enumeracion de cuentas).
     */
    public SesionTiendaDTO login(LoginTiendaDTO datos) {
        ClienteCuenta cuenta = cuentaRepository.findByEmail(normalizarEmail(datos.getEmail()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        if (!passwordEncoder.matches(datos.getPassword(), cuenta.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new DisabledException("La cuenta esta inactiva");
        }
        return emitirSesion(cuenta);
    }

    /**
     * Resuelve la cuenta autenticada desde el SecurityContext (el JwtAuthFilter deja
     * el id de cuenta como principal). Reutilizable por otros controladores de la
     * tienda, p. ej. el de pedidos web.
     */
    public ClienteCuenta cuentaActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadCredentialsException("No hay una sesion de tienda activa");
        }
        try {
            return cuentaRepository.findById(Integer.valueOf(auth.getName()))
                    .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                    .orElseThrow(() -> new BadCredentialsException("La cuenta ya no esta disponible"));
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("No hay una sesion de tienda activa");
        }
    }

    /** Perfil de la cuenta autenticada con sus tarjetas guardadas (solo datos no sensibles). */
    public CuentaTiendaDTO perfilActual() {
        ClienteCuenta cuenta = cuentaActual();
        List<TarjetaTiendaDTO> tarjetas = tarjetaRepository.findByCuentaIdOrderByIdAsc(cuenta.getId()).stream()
                .map(t -> new TarjetaTiendaDTO(t.getId(), t.getMarca(), t.getUltimos4(), t.getTitular()))
                .toList();
        return new CuentaTiendaDTO(cuenta.getCliente().getNombre(), cuenta.getEmail(),
                cuenta.getCliente().getTelefono(), tarjetas);
    }

    /**
     * Confirma el correo de la cuenta autenticada con el codigo recibido. Si ya estaba
     * verificado, es idempotente (devuelve ok). Un codigo incorrecto o vencido lanza
     * IllegalArgumentException (el controlador lo traduce a 400).
     */
    @Transactional
    public void verificarCorreo(String codigo) {
        ClienteCuenta cuenta = cuentaActual();
        if (Boolean.TRUE.equals(cuenta.getEmailVerificado())) {
            return;
        }
        if (!verificacionCorreoService.validar(cuenta.getEmail(), codigo)) {
            throw new IllegalArgumentException("El codigo es incorrecto o ha vencido");
        }
        cuenta.setEmailVerificado(true);
        cuentaRepository.save(cuenta);
    }

    /** Reenvia un codigo nuevo a la cuenta autenticada. */
    public void reenviarCodigo() {
        ClienteCuenta cuenta = cuentaActual();
        verificacionCorreoService.generarYEnviar(cuenta.getEmail());
    }

    private SesionTiendaDTO emitirSesion(ClienteCuenta cuenta) {
        String nombre = cuenta.getCliente().getNombre();
        String token = jwtService.emitir(cuenta.getId(), cuenta.getEmail(), nombre);
        return new SesionTiendaDTO(token, nombre, cuenta.getEmail(),
                Boolean.TRUE.equals(cuenta.getEmailVerificado()));
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
