package sv.edu.ues.alimentacion_hospitalaria.insumos.service;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.InsumoForm;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.InsumoVista;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;
import sv.edu.ues.alimentacion_hospitalaria.insumos.persistence.InsumoRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuDetalleInsumoRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.service.RegistroBitacoraService;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA')")
@Transactional(readOnly = true)
public class AdministracionInsumosService {

    private final InsumoRepository insumoRepository;
    private final MenuDetalleInsumoRepository detalles;
    private final RegistroBitacoraService bitacora;
    private final UsuarioRepository usuarios;

    public AdministracionInsumosService(
        InsumoRepository insumoRepository,
        MenuDetalleInsumoRepository detalles,
        RegistroBitacoraService bitacora,
        UsuarioRepository usuarios) {
        this.insumoRepository = insumoRepository;
        this.detalles = detalles;
        this.bitacora = bitacora;
        this.usuarios = usuarios;
    }

    public Page<InsumoVista> listar(int pagina, String busqueda) {
        var pageable = PageRequest.of(Math.max(0, pagina), 20, Sort.by("nombre", "id"));
        String filtro = busqueda == null ? "" : busqueda.trim();
        Page<Insumo> resultado = filtro.isEmpty()
            ? insumoRepository.findAll(pageable)
            : insumoRepository.findByNombreContainingIgnoreCase(filtro, pageable);
        List<Long> ids = resultado.getContent().stream().map(Insumo::getId).toList();
        Set<Long> enUso = ids.isEmpty() ? Set.of() : detalles.findInsumoIdsEnUso(ids);
        return resultado.map(insumo -> InsumoVista.desde(insumo, enUso.contains(insumo.getId())));
    }

    public InsumoForm consultar(Long id) {
        return InsumoForm.desde(buscar(id));
    }

    @Transactional
    public void crear(InsumoForm formulario) {
        Usuario actor = usuarioActual();
        validarPresentacion(formulario);
        String nombre = formulario.getNombre().trim();
        comprobarNombreDisponible(nombre, null);
        Insumo insumo = new Insumo(nombre, formulario.getUnidad().trim());
        actualizar(insumo, formulario);
        Insumo guardado = insumoRepository.save(insumo);
        bitacora.registrar("CREAR", "insumos", guardado.getId(), actor.getId());
    }

    @Transactional
    public void editar(Long id, InsumoForm formulario) {
        Usuario actor = usuarioActual();
        validarPresentacion(formulario);
        Insumo insumo = buscar(id);
        String nombre = formulario.getNombre().trim();
        comprobarNombreDisponible(nombre, id);
        insumo.setNombre(nombre);
        actualizar(insumo, formulario);
        bitacora.registrar("ACTUALIZAR", "insumos", id, actor.getId());
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario actor = usuarioActual();
        Insumo insumo = buscar(id);
        if (detalles.existsByInsumoId(id)) {
            throw new IllegalArgumentException("No se puede eliminar el insumo porque está asociado a uno o más menús.");
        }
        insumoRepository.delete(insumo);
        bitacora.registrar("ELIMINAR", "insumos", id, actor.getId());
    }

    private void actualizar(Insumo insumo, InsumoForm formulario) {
        insumo.setUnidad(formulario.getUnidad().trim());
        insumo.setMerma(formulario.getMermaPorcentaje().movePointLeft(2));
        insumo.setPresentacion(normalizarOpcional(formulario.getPresentacion()));
        insumo.setCantidadPresentacion(formulario.getCantidadPresentacion());
        insumo.setCostoUnitario(formulario.getCostoUnitario());
    }

    private void validarPresentacion(InsumoForm formulario) {
        if (!formulario.tienePresentacionCompleta()) {
            throw new IllegalArgumentException("Complete juntos la presentación y la cantidad por presentación.");
        }
    }

    private void comprobarNombreDisponible(String nombre, Long id) {
        insumoRepository.findByNombreIgnoreCase(nombre).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe un insumo con ese nombre.");
            }
        });
    }

    private Insumo buscar(Long id) {
        return insumoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El insumo solicitado no existe."));
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Usuario usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        Usuario usuario = usuarios.findByNombreUsuarioIgnoreCase(autenticacion.getName())
            .orElseThrow(() -> new AccessDeniedException("Acceso denegado."));
        Rol rol = usuario.getRol();
        if (!usuario.isActivo() || (rol != Rol.ROLE_ADMIN && rol != Rol.ROLE_NUTRICIONISTA)) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        return usuario;
    }
}
