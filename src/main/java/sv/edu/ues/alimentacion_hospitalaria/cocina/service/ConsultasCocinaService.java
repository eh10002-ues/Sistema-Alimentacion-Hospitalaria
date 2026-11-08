package sv.edu.ues.alimentacion_hospitalaria.cocina.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.MenusPorPacienteVista;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.MenusPorPacienteVista.EstadoAsignacion;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.OpcionConsulta;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.PacientesPorMenuVista;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuDetalleRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;

/**
 * SAH-6: consultas operativas de cocina (SC-03.1 y SC-03.2). Solo lectura.
 * Considera únicamente asignaciones aprobadas, como la proyección de insumos.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA', 'COCINA')")
public class ConsultasCocinaService {

    private final AsignacionMenuRepository asignaciones;
    private final MenuDetalleRepository detalles;
    private final PacienteRepository pacientes;

    public ConsultasCocinaService(AsignacionMenuRepository asignaciones, MenuDetalleRepository detalles,
                                  PacienteRepository pacientes) {
        this.asignaciones = asignaciones;
        this.detalles = detalles;
        this.pacientes = pacientes;
    }

    public List<OpcionConsulta> pacientesInternos() {
        return pacientes.findByEstadoOrderByApellidosAscNombresAscCodigoAsc(EstadoPaciente.INGRESADO).stream()
            .map(paciente -> new OpcionConsulta(paciente.getId(),
                paciente.getCodigo() + " — " + paciente.getApellidos() + ", " + paciente.getNombres()))
            .toList();
    }

    /** Incluye a los pacientes de todos los menús activos del régimen indicado. */
    public PacientesPorMenuVista pacientesPorRegimen(TipoDieta tipoDieta, LocalDate fecha) {
        List<PacientesPorMenuVista.Fila> filas = asignaciones
            .findVigentesPorTipoDieta(tipoDieta, EstadoMenu.ACTIVO, EstadoPaciente.INGRESADO, fecha).stream()
            .map(asignacion -> {
                Paciente paciente = asignacion.getPaciente();
                Cama cama = paciente.getCama();
                return new PacientesPorMenuVista.Fila(paciente.getCodigo(), nombreCompleto(paciente),
                    piso(cama), cama == null ? null : cama.getNumero(), asignacion.getMenu().getNombre(),
                    asignacion.getFechaInicio(), asignacion.getFechaFin());
            })
            .toList();
        return new PacientesPorMenuVista(tipoDieta, fecha, filas);
    }

    public MenusPorPacienteVista menusPorPaciente(Long pacienteId, LocalDate fecha) {
        Paciente paciente = pacientes.findById(pacienteId)
            .filter(encontrado -> encontrado.getEstado() == EstadoPaciente.INGRESADO)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe o no está ingresado."));
        List<AsignacionMenu> historial = asignaciones.findHistorialAprobadoPorPaciente(pacienteId);
        List<MenusPorPacienteVista.MenuVigente> vigentes = historial.stream()
            .filter(asignacion -> estado(asignacion, fecha) == EstadoAsignacion.VIGENTE)
            .map(this::menuVigente)
            .toList();
        List<MenusPorPacienteVista.Historial> filas = historial.stream()
            .map(asignacion -> new MenusPorPacienteVista.Historial(asignacion.getMenu().getNombre(),
                asignacion.getMenu().getTipoDieta().getEtiqueta(), asignacion.getFechaInicio(),
                asignacion.getFechaFin(), estado(asignacion, fecha)))
            .toList();
        Cama cama = paciente.getCama();
        return new MenusPorPacienteVista(paciente.getId(), paciente.getCodigo(), nombreCompleto(paciente),
            piso(cama), cama == null ? null : cama.getNumero(), fecha, vigentes, filas);
    }

    private MenusPorPacienteVista.MenuVigente menuVigente(AsignacionMenu asignacion) {
        Menu menu = asignacion.getMenu();
        // Se ordena en memoria: con hibernate.jdbc.time_zone=UTC, el ORDER BY de la base sobre hora_servicio
        // compara horas desplazadas y puede alterar el orden real del día.
        List<MenusPorPacienteVista.Tiempo> tiempos = detalles.findByMenuIdOrderByHoraServicioAsc(menu.getId())
            .stream()
            .sorted(Comparator.comparing(MenuDetalle::getHoraServicio)
                .thenComparing(MenuDetalle::getTiempoComida))
            .map(detalle -> new MenusPorPacienteVista.Tiempo(detalle.getTiempoComida().getEtiqueta(),
                detalle.getHoraServicio(), detalle.getPlatillo()))
            .toList();
        return new MenusPorPacienteVista.MenuVigente(menu.getNombre(), menu.getTipoDieta().getEtiqueta(),
            asignacion.getFechaInicio(), asignacion.getFechaFin(), tiempos);
    }

    private static EstadoAsignacion estado(AsignacionMenu asignacion, LocalDate fecha) {
        if (!asignacion.isActiva() || (asignacion.getFechaFin() != null && asignacion.getFechaFin().isBefore(fecha))) {
            return EstadoAsignacion.FINALIZADA;
        }
        if (asignacion.getFechaInicio().isAfter(fecha)) {
            return EstadoAsignacion.PROGRAMADA;
        }
        return asignacion.getMenu().getEstado() == EstadoMenu.ACTIVO
            ? EstadoAsignacion.VIGENTE
            : EstadoAsignacion.MENU_NO_DISPONIBLE;
    }

    private static String nombreCompleto(Paciente paciente) {
        return paciente.getNombres() + " " + paciente.getApellidos();
    }

    private static String piso(Cama cama) {
        return cama == null ? null : cama.getPiso().getCodigo() + " — " + cama.getPiso().getNombre();
    }
}
