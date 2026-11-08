package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.AlergiasSeverasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.AsignacionesPendientesReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.ComandasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.EncabezadoReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilaComanda;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.OpcionFiltro;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.PacientesSinMenuReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.persistence.ReportesRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

/**
 * SAH-9: reportes específicos. Solo lectura. Cada método repite la regla HTTP de su ruta en ReglasAcceso:
 * RE-01 usa los roles que consultan alergias, RE-02 los de las consultas de cocina (sin datos clínicos)
 * y RE-03/RE-04 los que consultan asignaciones.
 */
@Service
@Transactional(readOnly = true)
public class ReportesService {

    static final String CLINICOS = "hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA')";
    static final String CONSULTAS_COCINA = "hasAnyRole('ADMIN', 'NUTRICIONISTA', 'COCINA')";
    static final String ASIGNACIONES = "hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA')";

    /** Misma etiqueta que muestra la ficha del paciente para la severidad SEVERA. */
    static final String ETIQUETA_SEVERA = "Grave";
    static final String SIN_CAMA = "Sin cama asignada";

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportesRepository reportes;

    public ReportesService(ReportesRepository reportes) {
        this.reportes = reportes;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'COCINA')")
    public List<OpcionFiltro> pisos() {
        return reportes.buscarPisos().stream()
            .map(piso -> new OpcionFiltro(piso.getId(), etiqueta(piso)))
            .toList();
    }

    /** RE-01: alergias SEVERA de pacientes INGRESADO, agrupadas por piso. */
    @PreAuthorize(CLINICOS)
    public AlergiasSeverasReporte alergiasSeveras(Long pisoId) {
        String piso = filtroPiso(pisoId);
        Map<String, List<AlergiasSeverasReporte.Fila>> grupos = new LinkedHashMap<>();
        Set<Long> pacientes = new HashSet<>();
        List<PacienteAlergia> alergias = reportes.buscarAlergias(SeveridadAlergia.SEVERA, EstadoPaciente.INGRESADO, pisoId);
        for (PacienteAlergia alergia : alergias) {
            Paciente paciente = alergia.getPaciente();
            pacientes.add(paciente.getId());
            String etiquetaPiso = piso(paciente.getCama());
            grupos.computeIfAbsent(etiquetaPiso, clave -> new ArrayList<>())
                .add(new AlergiasSeverasReporte.Fila(etiquetaPiso, cama(paciente.getCama()), paciente.getCodigo(),
                    nombreCompleto(paciente), alergia.getNombre(), ETIQUETA_SEVERA));
        }
        List<AlergiasSeverasReporte.GrupoPiso> pisos = grupos.entrySet().stream()
            .map(grupo -> new AlergiasSeverasReporte.GrupoPiso(grupo.getKey(), List.copyOf(grupo.getValue())))
            .toList();
        EncabezadoReporte encabezado = encabezado("RE-01", "Pacientes con alergias severas registradas por piso",
            "re-01-alergias-severas-" + hoy(), List.of("Pacientes ingresados", "Piso: " + piso),
            "Se listan las alergias de severidad grave tal como fueron registradas en la ficha del paciente. "
                + "El sistema no distingue el tipo de alergia (alimentaria, medicamentosa u otra).");
        return new AlergiasSeverasReporte(encabezado, pisos, pacientes.size(), alergias.size());
    }

    /** RE-02: raciones de asignaciones vigentes por tiempo de comida. Omite horas y datos clínicos. */
    @PreAuthorize(CONSULTAS_COCINA)
    public ComandasReporte comandas(LocalDate fecha, Long pisoId, TiempoComida tiempoComida) {
        String piso = filtroPiso(pisoId);
        // El orden del día sale del enum TiempoComida, no de hora_servicio.
        List<FilaComanda> filas = reportes.contarRaciones(EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO, fecha, pisoId,
                tiempoComida).stream()
            .sorted(Comparator.comparing(FilaComanda::tiempoComida)
                .thenComparing(FilaComanda::platillo)
                .thenComparing(FilaComanda::menu)
                .thenComparing(FilaComanda::menuId)
                .thenComparing(FilaComanda::pisoCodigo, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
        Map<TiempoComida, List<ComandasReporte.Fila>> grupos = new LinkedHashMap<>();
        for (FilaComanda fila : filas) {
            grupos.computeIfAbsent(fila.tiempoComida(), clave -> new ArrayList<>())
                .add(new ComandasReporte.Fila(fila.platillo(), fila.menu(), fila.tipoDieta().getEtiqueta(),
                    fila.pisoCodigo() == null ? SIN_CAMA : fila.pisoCodigo() + " — " + fila.pisoNombre(),
                    fila.raciones()));
        }
        List<ComandasReporte.GrupoTiempo> tiempos = grupos.entrySet().stream()
            .map(grupo -> new ComandasReporte.GrupoTiempo(grupo.getKey().getEtiqueta(), List.copyOf(grupo.getValue()),
                grupo.getValue().stream().mapToLong(ComandasReporte.Fila::raciones).sum()))
            .toList();
        long total = tiempos.stream().mapToLong(ComandasReporte.GrupoTiempo::subtotal).sum();
        EncabezadoReporte encabezado = encabezado("RE-02", "Comandas de producción por tiempo de comida",
            "re-02-comandas-" + fecha,
            List.of("Fecha: " + fecha.format(FECHA),
                "Tiempo de comida: " + (tiempoComida == null ? "Todos" : tiempoComida.getEtiqueta()),
                "Piso: " + piso),
            "Raciones de las asignaciones de menú activas, aprobadas y vigentes en la fecha, de pacientes ingresados "
                + "y menús activos. Las horas de servicio se omiten temporalmente.");
        return new ComandasReporte(encabezado, fecha, tiempos, total);
    }

    /** RE-03: asignaciones de menú activas sin aprobar de pacientes INGRESADO. No son prescripciones dietéticas. */
    @PreAuthorize(ASIGNACIONES)
    public AsignacionesPendientesReporte asignacionesPendientes() {
        List<AsignacionesPendientesReporte.Fila> filas = reportes
            .buscarAsignacionesPendientes(EstadoPaciente.INGRESADO).stream()
            .map(asignacion -> {
                Paciente paciente = asignacion.getPaciente();
                Usuario asignador = asignacion.getAsignadoPor();
                return new AsignacionesPendientesReporte.Fila(paciente.getCodigo(), nombreCompleto(paciente),
                    piso(paciente.getCama()), cama(paciente.getCama()), asignacion.getMenu().getNombre(),
                    asignacion.getMenu().getTipoDieta().getEtiqueta(), asignacion.getFechaInicio(),
                    asignacion.getFechaFin(),
                    asignador.getNombreCompleto() + " (" + asignador.getRol().getEtiqueta() + ")",
                    asignacion.getCreatedAt());
            })
            .toList();
        EncabezadoReporte encabezado = encabezado("RE-03", "Asignaciones de menú pendientes de aprobación",
            "re-03-asignaciones-pendientes-" + hoy(),
            List.of("Asignaciones de menú activas sin aprobar", "Pacientes ingresados"),
            "Este reporte consulta asignaciones de menú pendientes de aprobación. No corresponde a prescripciones "
                + "dietéticas pendientes: en el sistema, toda prescripción dietética se registra ya aprobada.");
        return new AsignacionesPendientesReporte(encabezado, filas);
    }

    /**
     * RE-04 (propuesta de sustitución del reporte de interacciones fármaco-nutriente, pendiente de validación):
     * pacientes INGRESADO sin asignación activa, aprobada y vigente en la fecha. La prescripción dietética
     * de SAH-2 no se considera asignación de menú.
     */
    @PreAuthorize(ASIGNACIONES)
    public PacientesSinMenuReporte pacientesSinMenu(LocalDate fecha, Long pisoId) {
        String piso = filtroPiso(pisoId);
        List<Paciente> pacientes = reportes.buscarPacientesSinMenuVigente(EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO,
            fecha, pisoId);
        List<Long> ids = pacientes.stream().map(Paciente::getId).toList();
        Set<Long> conPendiente = ids.isEmpty() ? Set.of() : Set.copyOf(reportes.buscarPacientesConAsignacionPendiente(ids));
        Map<Long, AsignacionMenu> ultimaAprobada = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            // Llegan ordenadas de la más reciente a la más antigua: se conserva la primera de cada paciente.
            reportes.buscarAsignacionesAprobadas(ids)
                .forEach(asignacion -> ultimaAprobada.putIfAbsent(asignacion.getPaciente().getId(), asignacion));
        }
        List<PacientesSinMenuReporte.Fila> filas = pacientes.stream()
            .map(paciente -> {
                AsignacionMenu ultima = ultimaAprobada.get(paciente.getId());
                return new PacientesSinMenuReporte.Fila(piso(paciente.getCama()), cama(paciente.getCama()),
                    paciente.getCodigo(), nombreCompleto(paciente), conPendiente.contains(paciente.getId()),
                    ultima == null ? null : ultima.getMenu().getNombre(),
                    ultima == null ? null : ultima.getFechaInicio(),
                    ultima == null ? null : ultima.getFechaFin());
            })
            .toList();
        long pendientes = filas.stream().filter(PacientesSinMenuReporte.Fila::asignacionPendiente).count();
        EncabezadoReporte encabezado = encabezado("RE-04", "Pacientes ingresados sin menú vigente aprobado",
            "re-04-pacientes-sin-menu-" + fecha, List.of("Fecha: " + fecha.format(FECHA), "Piso: " + piso),
            "Pacientes ingresados sin una asignación de menú activa, aprobada y vigente en la fecha, con un menú activo. "
                + "La prescripción dietética no se considera asignación de menú.");
        return new PacientesSinMenuReporte(encabezado, fecha, filas, pendientes);
    }

    private String filtroPiso(Long pisoId) {
        if (pisoId == null) {
            return "Todos los pisos";
        }
        return reportes.buscarPiso(pisoId).map(ReportesService::etiqueta)
            .orElseThrow(() -> new ResourceNotFoundException("El piso solicitado no existe."));
    }

    private static EncabezadoReporte encabezado(String codigo, String titulo, String archivo, List<String> filtros,
                                                String nota) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return new EncabezadoReporte(codigo, titulo, archivo, filtros, nota,
            autenticacion == null ? "" : autenticacion.getName(), Instant.now());
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZONA_EL_SALVADOR);
    }

    private static String nombreCompleto(Paciente paciente) {
        return paciente.getNombres() + " " + paciente.getApellidos();
    }

    private static String etiqueta(Piso piso) {
        return piso.getCodigo() + " — " + piso.getNombre();
    }

    private static String piso(Cama cama) {
        return cama == null ? SIN_CAMA : etiqueta(cama.getPiso());
    }

    private static String cama(Cama cama) {
        return cama == null ? "—" : cama.getNumero();
    }
}
