package sv.edu.ues.alimentacion_hospitalaria.menus.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.service.RegistroBitacoraService;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;
import sv.edu.ues.alimentacion_hospitalaria.insumos.persistence.InsumoRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalleInsumo;
import sv.edu.ues.alimentacion_hospitalaria.menus.dto.InsumoDetalleForm;
import sv.edu.ues.alimentacion_hospitalaria.menus.dto.MenuDetalleForm;
import sv.edu.ues.alimentacion_hospitalaria.menus.dto.MenuForm;
import sv.edu.ues.alimentacion_hospitalaria.menus.dto.VistasMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/**
 * Administración de menús y de sus tiempos de comida. El tipo de dieta usa el enum TipoDieta
 * y el horario es la hora de servicio de cada detalle. La consulta está abierta a los cinco roles
 * (no contiene datos de pacientes); la modificación, a Administración y Nutrición.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO', 'COCINA')")
public class AdministracionMenusService {

    private static final String ADMINISTRAR = "hasAnyRole('ADMIN', 'NUTRICIONISTA')";
    private static final String TABLA_MENUS = "menus";
    private static final String TABLA_DETALLES = "menu_detalles";

    // Orden en memoria: con hibernate.jdbc.time_zone=UTC el orden de la base sobre TIME no es fiable.
    private static final Comparator<MenuDetalle> ORDEN_DETALLES = Comparator
        .comparing(MenuDetalle::getHoraServicio).thenComparing(MenuDetalle::getTiempoComida);

    private final MenuRepository menuRepository;
    private final InsumoRepository insumoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroBitacoraService bitacora;

    public AdministracionMenusService(MenuRepository menuRepository, InsumoRepository insumoRepository,
                                      UsuarioRepository usuarioRepository, RegistroBitacoraService bitacora) {
        this.menuRepository = menuRepository;
        this.insumoRepository = insumoRepository;
        this.usuarioRepository = usuarioRepository;
        this.bitacora = bitacora;
    }

    public List<VistasMenu.Resumen> listar() {
        return menuRepository.findAll(Sort.by("nombre", "id")).stream()
            .map(menu -> new VistasMenu.Resumen(menu.getId(), menu.getNombre(), menu.getTipo(),
                menu.getTipoDieta(), menu.getEstado(), menu.getDetalles().size()))
            .toList();
    }

    public VistasMenu.Menu consultar(Long id) {
        Menu menu = buscar(id);
        List<VistasMenu.Detalle> detalles = menu.getDetalles().stream().sorted(ORDEN_DETALLES)
            .map(this::vista)
            .toList();
        return new VistasMenu.Menu(menu.getId(), menu.getNombre(), menu.getTipo(), menu.getTipoDieta(),
            menu.getEstado(), detalles);
    }

    @PreAuthorize(ADMINISTRAR)
    public MenuForm formulario(Long id) {
        return MenuForm.desde(buscar(id));
    }

    @PreAuthorize(ADMINISTRAR)
    public MenuDetalleForm formularioDetalle(Long menuId, Long detalleId) {
        return MenuDetalleForm.desde(buscarDetalle(buscar(menuId), detalleId));
    }

    @PreAuthorize(ADMINISTRAR)
    public VistasMenu.Detalle consultarDetalle(Long menuId, Long detalleId) {
        return vista(buscarDetalle(buscar(menuId), detalleId));
    }

    @PreAuthorize(ADMINISTRAR)
    public List<VistasMenu.OpcionInsumo> opcionesInsumo() {
        return insumoRepository.findAll(Sort.by("nombre")).stream()
            .map(insumo -> new VistasMenu.OpcionInsumo(insumo.getId(), insumo.getNombre(), insumo.getUnidad()))
            .toList();
    }

    /** Los menús nuevos quedan en BORRADOR hasta que se activan con al menos un tiempo de comida. */
    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public Long crear(MenuForm formulario) {
        Menu menu = menuRepository.save(new Menu(formulario.getNombre().trim(), formulario.getTipo(),
            formulario.getTipoDieta()));
        registrar("CREAR", TABLA_MENUS, menu.getId());
        return menu.getId();
    }

    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void editar(Long id, MenuForm formulario) {
        Menu menu = buscar(id);
        menu.setNombre(formulario.getNombre().trim());
        menu.setTipo(formulario.getTipo());
        menu.setTipoDieta(formulario.getTipoDieta());
        registrar("ACTUALIZAR", TABLA_MENUS, id);
    }

    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void activar(Long id) {
        Menu menu = buscar(id);
        if (menu.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Agregue al menos un tiempo de comida antes de activar el menú.");
        }
        menu.setEstado(EstadoMenu.ACTIVO);
        registrar("ACTIVAR", TABLA_MENUS, id);
    }

    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void desactivar(Long id) {
        buscar(id).setEstado(EstadoMenu.INACTIVO);
        registrar("DESACTIVAR", TABLA_MENUS, id);
    }

    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void agregarDetalle(Long menuId, MenuDetalleForm formulario) {
        Menu menu = buscar(menuId);
        validarTiempoDisponible(menu, formulario, null);
        menu.agregarDetalle(new MenuDetalle(formulario.getTiempoComida(), formulario.getHoraServicio(),
            formulario.getPlatillo().trim()));
        registrar("ACTUALIZAR", TABLA_MENUS, menuId);
    }

    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void editarDetalle(Long menuId, Long detalleId, MenuDetalleForm formulario) {
        Menu menu = buscar(menuId);
        MenuDetalle detalle = buscarDetalle(menu, detalleId);
        validarTiempoDisponible(menu, formulario, detalle);
        detalle.setTiempoComida(formulario.getTiempoComida());
        detalle.setHoraServicio(formulario.getHoraServicio());
        detalle.setPlatillo(formulario.getPlatillo().trim());
        registrar("ACTUALIZAR", TABLA_DETALLES, detalleId);
    }

    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void eliminarDetalle(Long menuId, Long detalleId) {
        Menu menu = buscar(menuId);
        MenuDetalle detalle = buscarDetalle(menu, detalleId);
        if (menu.getEstado() == EstadoMenu.ACTIVO && menu.getDetalles().size() == 1) {
            throw new IllegalArgumentException(
                "Un menú activo debe conservar al menos un tiempo de comida. Desactívelo antes de quitarlo.");
        }
        menu.quitarDetalle(detalle);
        registrar("ACTUALIZAR", TABLA_MENUS, menuId);
    }

    /** Agrega el insumo al tiempo de comida o actualiza su gramaje si ya estaba asociado. */
    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void guardarInsumo(Long menuId, Long detalleId, InsumoDetalleForm formulario) {
        MenuDetalle detalle = buscarDetalle(buscar(menuId), detalleId);
        Insumo insumo = insumoRepository.findById(formulario.getInsumoId())
            .orElseThrow(() -> new IllegalArgumentException("El insumo seleccionado no existe."));
        detalle.getInsumos().stream()
            .filter(existente -> existente.getInsumo().getId().equals(insumo.getId()))
            .findFirst()
            .ifPresentOrElse(existente -> existente.setCantidadPorRacion(formulario.getCantidadPorRacion()),
                () -> detalle.agregarInsumo(new MenuDetalleInsumo(insumo, formulario.getCantidadPorRacion())));
        registrar("ACTUALIZAR", TABLA_DETALLES, detalleId);
    }

    @Transactional
    @PreAuthorize(ADMINISTRAR)
    public void quitarInsumo(Long menuId, Long detalleId, Long detalleInsumoId) {
        MenuDetalle detalle = buscarDetalle(buscar(menuId), detalleId);
        MenuDetalleInsumo insumo = detalle.getInsumos().stream()
            .filter(existente -> existente.getId().equals(detalleInsumoId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "El insumo indicado no pertenece a este tiempo de comida."));
        detalle.quitarInsumo(insumo);
        registrar("ACTUALIZAR", TABLA_DETALLES, detalleId);
    }

    private void validarTiempoDisponible(Menu menu, MenuDetalleForm formulario, MenuDetalle actual) {
        boolean repetido = menu.getDetalles().stream()
            .anyMatch(otro -> otro != actual && otro.getTiempoComida() == formulario.getTiempoComida());
        if (repetido) {
            throw new IllegalArgumentException("El menú ya tiene registrado el tiempo de comida "
                + formulario.getTiempoComida().getEtiqueta() + ".");
        }
    }

    private VistasMenu.Detalle vista(MenuDetalle detalle) {
        List<VistasMenu.Insumo> insumos = detalle.getInsumos().stream()
            .map(item -> new VistasMenu.Insumo(item.getId(), item.getInsumo().getNombre(),
                item.getInsumo().getUnidad(), item.getCantidadPorRacion()))
            .sorted(Comparator.comparing(VistasMenu.Insumo::nombre))
            .toList();
        return new VistasMenu.Detalle(detalle.getId(), detalle.getTiempoComida(), detalle.getHoraServicio(),
            detalle.getPlatillo(), insumos);
    }

    private Menu buscar(Long id) {
        return menuRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El menú solicitado no existe."));
    }

    private MenuDetalle buscarDetalle(Menu menu, Long detalleId) {
        return menu.getDetalles().stream()
            .filter(detalle -> detalle.getId().equals(detalleId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "El tiempo de comida solicitado no existe en este menú."));
    }

    private void registrar(String accion, String tabla, Long id) {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId = usuarioRepository.findByNombreUsuarioIgnoreCase(autenticacion.getName())
            .orElseThrow(() -> new AccessDeniedException("Acceso denegado."))
            .getId();
        bitacora.registrar(accion, tabla, id, usuarioId);
    }
}
