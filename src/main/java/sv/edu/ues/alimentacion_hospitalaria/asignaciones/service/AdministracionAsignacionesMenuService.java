package sv.edu.ues.alimentacion_hospitalaria.asignaciones.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto.AsignacionMenuForm;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto.OrdenMenuCocina;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto.VistasAsignacion;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.service.RegistroBitacoraService;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/**
 * Asignación de menús a pacientes y su aprobación. Las asignaciones nacen pendientes
 * (sin aprobador) y solo cuentan para cocina y reportes después de aprobarse.
 *
 * Aprobación: la regla HTTP permite Administración y Nutrición, y el modelo (V8) exige un
 * aprobador con rol Médico o Nutricionista. Se aplica la intersección: solo Nutricionista.
 * No existe un estado de rechazo en el modelo, por lo que esa operación no se ofrece.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA')")
public class AdministracionAsignacionesMenuService {

    private static final String TABLA = "asignaciones_menu";
    private static final Set<Rol> ROLES_APROBADORES = EnumSet.of(Rol.ROLE_MEDICO, Rol.ROLE_NUTRICIONISTA);
    // Límite del tipo DATE de MySQL; representa un período sin fecha de fin.
    private static final LocalDate SIN_FIN = LocalDate.of(9999, 12, 31);

    private final AsignacionMenuRepository asignacionMenuRepository;
    private final PacienteRepository pacienteRepository;
    private final MenuRepository menuRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroBitacoraService bitacora;
    private final ApplicationEventPublisher eventos;

    public AdministracionAsignacionesMenuService(AsignacionMenuRepository asignacionMenuRepository,
                                                 PacienteRepository pacienteRepository,
                                                 MenuRepository menuRepository,
                                                 UsuarioRepository usuarioRepository,
                                                 RegistroBitacoraService bitacora,
                                                 ApplicationEventPublisher eventos) {
        this.asignacionMenuRepository = asignacionMenuRepository;
        this.pacienteRepository = pacienteRepository;
        this.menuRepository = menuRepository;
        this.usuarioRepository = usuarioRepository;
        this.bitacora = bitacora;
        this.eventos = eventos;
    }

    public List<VistasAsignacion.Fila> listar(boolean soloPendientes) {
        return asignacionMenuRepository.findActivasParaAdministracion(EstadoPaciente.INGRESADO, soloPendientes)
            .stream().map(this::fila).toList();
    }

    public List<VistasAsignacion.Opcion> pacientesIngresados() {
        return pacienteRepository.findByEstadoOrderByApellidosAscNombresAscCodigoAsc(EstadoPaciente.INGRESADO)
            .stream()
            .map(paciente -> new VistasAsignacion.Opcion(paciente.getId(),
                paciente.getCodigo() + " — " + paciente.getApellidos() + ", " + paciente.getNombres()))
            .toList();
    }

    public List<VistasAsignacion.Opcion> menusActivos() {
        return menuRepository.findAll(Sort.by("nombre", "id")).stream()
            .filter(menu -> menu.getEstado() == EstadoMenu.ACTIVO)
            .map(menu -> new VistasAsignacion.Opcion(menu.getId(),
                menu.getNombre() + " (" + menu.getTipoDieta().getEtiqueta() + ")"))
            .toList();
    }

    @Transactional
    public Long crear(AsignacionMenuForm formulario, LocalDate hoy) {
        Paciente paciente = pacienteRepository.findById(formulario.getPacienteId())
            .orElseThrow(() -> new IllegalArgumentException("El paciente seleccionado no existe."));
        if (paciente.getEstado() != EstadoPaciente.INGRESADO) {
            throw new IllegalArgumentException("Solo se pueden asignar menús a pacientes ingresados.");
        }
        Menu menu = menuRepository.findById(formulario.getMenuId())
            .orElseThrow(() -> new IllegalArgumentException("El menú seleccionado no existe."));
        if (menu.getEstado() != EstadoMenu.ACTIVO) {
            throw new IllegalArgumentException("Solo se pueden asignar menús activos.");
        }
        LocalDate inicio = formulario.getFechaInicio();
        LocalDate fin = formulario.getFechaFin();
        if (inicio.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a hoy.");
        }
        if (fin != null && fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
        if (asignacionMenuRepository.existeActivaEnPeriodo(paciente.getId(), inicio, fin == null ? SIN_FIN : fin)) {
            throw new IllegalArgumentException(
                "El paciente ya tiene una asignación de menú activa o pendiente que se cruza con esas fechas.");
        }
        Usuario usuario = usuarioActual();
        AsignacionMenu asignacion = new AsignacionMenu(paciente, menu, usuario, inicio);
        asignacion.setFechaFin(fin);
        asignacion = asignacionMenuRepository.save(asignacion);
        bitacora.registrar("CREAR", TABLA, asignacion.getId(), usuario.getId());
        return asignacion.getId();
    }

    @Transactional
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public void aprobar(Long id, LocalDate hoy) {
        AsignacionMenu asignacion = asignacionMenuRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("La asignación de menú solicitada no existe."));
        if (asignacion.getAprobadoPor() != null) {
            throw new IllegalArgumentException("La asignación ya fue aprobada.");
        }
        if (!asignacion.isActiva()) {
            throw new IllegalArgumentException("La asignación no está activa.");
        }
        if (asignacion.getPaciente().getEstado() != EstadoPaciente.INGRESADO) {
            throw new IllegalArgumentException("El paciente ya no está ingresado.");
        }
        if (asignacion.getMenu().getEstado() != EstadoMenu.ACTIVO) {
            throw new IllegalArgumentException("El menú asignado ya no está activo.");
        }
        if (asignacion.getFechaFin() != null && asignacion.getFechaFin().isBefore(hoy)) {
            throw new IllegalArgumentException("El período de la asignación ya terminó.");
        }
        Usuario aprobador = usuarioActual();
        if (!ROLES_APROBADORES.contains(aprobador.getRol())) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        asignacion.setAprobadoPor(aprobador);
        asignacion.setFechaAprobacion(hoy);
        bitacora.registrar("APROBAR", TABLA, asignacion.getId(), aprobador.getId());
        // Cocina solo recibe las asignaciones que ya están vigentes; las futuras aparecen en la carga de su fecha.
        if (!asignacion.getFechaInicio().isAfter(hoy)) {
            eventos.publishEvent(new AsignacionMenuAprobada(OrdenMenuCocina.desde(asignacion)));
        }
    }

    private VistasAsignacion.Fila fila(AsignacionMenu asignacion) {
        Paciente paciente = asignacion.getPaciente();
        String ubicacion = paciente.getCama() == null ? "Sin cama asignada"
            : paciente.getCama().getPiso().getCodigo() + " · Cama " + paciente.getCama().getNumero();
        Usuario aprobador = asignacion.getAprobadoPor();
        return new VistasAsignacion.Fila(asignacion.getId(), paciente.getCodigo(),
            paciente.getApellidos() + ", " + paciente.getNombres(), ubicacion, asignacion.getMenu().getNombre(),
            asignacion.getMenu().getTipoDieta(), asignacion.getFechaInicio(), asignacion.getFechaFin(),
            asignacion.getAsignadoPor().getNombreCompleto(),
            aprobador == null ? null : aprobador.getNombreCompleto(), asignacion.getFechaAprobacion());
    }

    private Usuario usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        return usuarioRepository.findByNombreUsuarioIgnoreCase(autenticacion.getName())
            .orElseThrow(() -> new AccessDeniedException("Acceso denegado."));
    }
}
