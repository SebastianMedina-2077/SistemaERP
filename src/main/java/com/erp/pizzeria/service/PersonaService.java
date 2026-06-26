package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.EmpleadoFormDTO;
import com.erp.pizzeria.dto.ProveedorFormDTO;
import com.erp.pizzeria.dto.UsuarioFormDTO;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Cliente;
import com.erp.pizzeria.model.Empleado;
import com.erp.pizzeria.model.Proveedor;
import com.erp.pizzeria.model.Rol;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.ClienteRepository;
import com.erp.pizzeria.repository.CompraRepository;
import com.erp.pizzeria.repository.EmpleadoRepository;
import com.erp.pizzeria.repository.ProveedorRepository;
import com.erp.pizzeria.repository.MovimientoRepository;
import com.erp.pizzeria.repository.PedidoRepository;
import com.erp.pizzeria.repository.RolRepository;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.audit.Audit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PersonaService {

    private final RolRepository rolRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final ClienteRepository clienteRepository;
    private final CompraRepository compraRepository;
    private final PedidoRepository pedidoRepository;
    private final MovimientoRepository movimientoRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonaService(RolRepository rolRepository,
                          EmpleadoRepository empleadoRepository,
                          UsuarioRepository usuarioRepository,
                          ProveedorRepository proveedorRepository,
                          ClienteRepository clienteRepository,
                          CompraRepository compraRepository,
                          PedidoRepository pedidoRepository,
                          MovimientoRepository movimientoRepository,
                          PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.proveedorRepository = proveedorRepository;
        this.clienteRepository = clienteRepository;
        this.compraRepository = compraRepository;
        this.pedidoRepository = pedidoRepository;
        this.movimientoRepository = movimientoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Rol> listRoles() {
        return rolRepository.findAll();
    }

    public List<Empleado> listEmpleados() {
        return empleadoRepository.findAll();
    }

    public List<Usuario> listUsuarios() {
        return usuarioRepository.findAllConRelaciones();
    }

    public List<Proveedor> listProveedores() {
        return proveedorRepository.findAll();
    }

    public List<Cliente> listClientes() {
        return clienteRepository.findAll();
    }

    // ---- CRUD de proveedores ---------------------------------------

    public Proveedor getProveedor(Integer idProveedor) {
        return proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", idProveedor));
    }

    public boolean rucEnUso(String ruc, Integer idExcluir) {
        return idExcluir == null
                ? proveedorRepository.existsByRuc(ruc)
                : proveedorRepository.existsByRucAndIdProveedorNot(ruc, idExcluir);
    }

    @Audit(accion = "CREAR", entidad = "Proveedor")
    @Transactional
    public Proveedor crearProveedor(ProveedorFormDTO form) {
        Proveedor proveedor = new Proveedor();
        form.applyTo(proveedor);
        return proveedorRepository.save(proveedor);
    }

    @Audit(accion = "EDITAR", entidad = "Proveedor")
    @Transactional
    public Proveedor actualizarProveedor(Integer idProveedor, ProveedorFormDTO form) {
        Proveedor proveedor = getProveedor(idProveedor);
        form.applyTo(proveedor);
        return proveedorRepository.save(proveedor);
    }

    // ---- CRUD de empleados -------------------------------------------

    public Empleado getEmpleado(Integer idEmpleado) {
        return empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> ResourceNotFoundException.of("Empleado", idEmpleado));
    }

    public boolean dniEnUso(String dni, Integer idExcluir) {
        return idExcluir == null
                ? empleadoRepository.existsByDni(dni)
                : empleadoRepository.existsByDniAndIdEmpleadoNot(dni, idExcluir);
    }

    @Audit(accion = "CREAR", entidad = "Empleado")
    @Transactional
    public Empleado crearEmpleado(EmpleadoFormDTO form) {
        Empleado empleado = new Empleado();
        form.applyTo(empleado);
        return empleadoRepository.save(empleado);
    }

    @Audit(accion = "EDITAR", entidad = "Empleado")
    @Transactional
    public Empleado actualizarEmpleado(Integer idEmpleado, EmpleadoFormDTO form) {
        Empleado empleado = getEmpleado(idEmpleado);
        form.applyTo(empleado);
        return empleadoRepository.save(empleado);
    }

    @Audit(accion = "ELIMINAR", entidad = "Empleado")
    @Transactional
    public void eliminarEmpleado(Integer idEmpleado) {
        Empleado empleado = getEmpleado(idEmpleado);
        if (usuarioRepository.existsByEmpleado_IdEmpleado(idEmpleado)) {
            throw new IllegalStateException("'" + empleado.getNombre() + " " + empleado.getApellido()
                    + "' tiene un usuario de sistema vinculado. Elimina o desvincula el usuario primero.");
        }
        empleadoRepository.delete(empleado);
    }

    // ---- CRUD de usuarios --------------------------------------------

    public Usuario getUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", idUsuario));
    }

    public boolean usernameEnUso(String username, Integer idExcluir) {
        return idExcluir == null
                ? usuarioRepository.existsByUsernameIgnoreCase(username)
                : usuarioRepository.existsByUsernameIgnoreCaseAndIdUsuarioNot(username, idExcluir);
    }

    @Audit(accion = "CREAR", entidad = "Usuario")
    @Transactional
    public Usuario crearUsuario(UsuarioFormDTO form) {
        Usuario usuario = new Usuario();
        usuario.setPassword(passwordEncoder.encode(form.getPassword()));
        return guardarUsuario(usuario, form);
    }

    @Audit(accion = "EDITAR", entidad = "Usuario")
    @Transactional
    public Usuario actualizarUsuario(Integer idUsuario, UsuarioFormDTO form) {
        Usuario usuario = getUsuario(idUsuario);
        if (Boolean.TRUE.equals(usuario.getEsAdminSupremo())) {
            throw new IllegalStateException("El administrador supremo no puede ser modificado.");
        }
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        return guardarUsuario(usuario, form);
    }

    private Usuario guardarUsuario(Usuario usuario, UsuarioFormDTO form) {
        usuario.setUsername(form.getUsername().trim());
        usuario.setEstado(form.isEstado());
        usuario.setRol(rolRepository.findById(form.getIdRol())
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", form.getIdRol())));
        usuario.setEmpleado(form.getIdEmpleado() != null ? getEmpleado(form.getIdEmpleado()) : null);
        return usuarioRepository.save(usuario);
    }

    @Audit(accion = "ESTADO", entidad = "Usuario")
    @Transactional
    public Usuario cambiarEstadoUsuario(Integer idUsuario, boolean estado) {
        Usuario usuario = getUsuario(idUsuario);
        if (Boolean.TRUE.equals(usuario.getEsAdminSupremo())) {
            throw new IllegalStateException("El administrador supremo no puede ser modificado.");
        }
        usuario.setEstado(estado);
        return usuarioRepository.save(usuario);
    }

    @Audit(accion = "ELIMINAR", entidad = "Usuario")
    @Transactional
    public void eliminarUsuario(Integer idUsuario) {
        Usuario usuario = getUsuario(idUsuario);
        if (Boolean.TRUE.equals(usuario.getEsAdminSupremo())) {
            throw new IllegalStateException("El administrador supremo no puede ser eliminado.");
        }
        if (pedidoRepository.existsByUsuario_IdUsuario(idUsuario)
                || compraRepository.existsByUsuario_IdUsuario(idUsuario)
                || movimientoRepository.existsByUsuario_IdUsuario(idUsuario)) {
            throw new IllegalStateException("'" + usuario.getUsername()
                    + "' tiene actividad registrada (pedidos, compras o movimientos). Desactivalo en su lugar.");
        }
        usuarioRepository.delete(usuario);
    }

    @Audit(accion = "ELIMINAR", entidad = "Proveedor")
    @Transactional
    public void eliminarProveedor(Integer idProveedor) {
        Proveedor proveedor = getProveedor(idProveedor);
        if (compraRepository.existsByProveedor_IdProveedor(idProveedor)) {
            throw new IllegalStateException("'" + proveedor.getNombre()
                    + "' tiene compras registradas y no puede eliminarse.");
        }
        proveedorRepository.delete(proveedor);
    }
}
