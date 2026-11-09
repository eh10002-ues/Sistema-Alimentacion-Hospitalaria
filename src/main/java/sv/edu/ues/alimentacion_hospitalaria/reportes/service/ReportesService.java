package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.AlergiasSeverasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.AsignacionesPendientesReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.ComandasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.EncabezadoReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.ExpedientePacienteReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.HojaRutaEntregaReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.HistorialDietasPacienteReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.HojaTecnicaMenuReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilaComanda;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.OpcionFiltro;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.PacientesSinMenuReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.persistence.ReportesRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.EstadoPrescripcion;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.AsignacionPiso;
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
    private final MenuRepository menus;

    public ReportesService(ReportesRepository reportes, MenuRepository menus) {
        this.reportes = reportes;
        this.menus = menus;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'COCINA')")
    public List<OpcionFiltro> pisos() {
        return reportes.buscarPisos().stream()
            .map(piso -> new OpcionFiltro(piso.getId(), etiqueta(piso)))
            .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'COCINA')")
    public List<OpcionFiltro> menus() {
        return menus.findAll().stream()
            .sorted(Comparator.comparing(Menu::getNombre, String.CASE_INSENSITIVE_ORDER))
            .map(menu -> new OpcionFiltro(menu.getId(), menu.getNombre() + " — " + menu.getTipoDieta().getEtiqueta()))
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

    /** RD-01: expediente clínico-nutricional de un paciente identificado por su código. */
    @PreAuthorize(CLINICOS)
    public ExpedientePacienteReporte expedientePaciente(String codigo, LocalDate fechaDesde, LocalDate fechaHasta,
                                                        String estadoAsignacion) {
        Paciente paciente = reportes.buscarPacientePorCodigo(codigo)
            .orElseThrow(() -> new ResourceNotFoundException("No existe un paciente con el código indicado."));
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        Boolean activa = switch (estadoAsignacion == null ? "" : estadoAsignacion.trim().toUpperCase()) {
            case "ACTIVA" -> Boolean.TRUE;
            case "INACTIVA" -> Boolean.FALSE;
            default -> null;
        };
        List<AsignacionMenu> asignaciones = reportes.buscarAsignacionesPaciente(paciente.getId(), activa,
            fechaDesde, fechaHasta);

        List<String> patologias = paciente.getEnfermedades().stream()
            .map(enfermedad -> enfermedad.getNombre())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        List<ExpedientePacienteReporte.Medicamento> medicamentos = paciente.getMedicamentos().stream()
            .sorted(Comparator.comparing(medicamento -> medicamento.getNombre(), String.CASE_INSENSITIVE_ORDER))
            .map(medicamento -> new ExpedientePacienteReporte.Medicamento(medicamento.getNombre(),
                medicamento.getDosis(), medicamento.getFrecuenciaHoras(),
                FECHA_HORA.format(medicamento.getHoraPrimeraDosis())))
            .toList();
        List<ExpedientePacienteReporte.Alergia> alergias = paciente.getAlergias().stream()
            .sorted(Comparator.comparing(alergia -> alergia.getNombre(), String.CASE_INSENSITIVE_ORDER))
            .map(alergia -> new ExpedientePacienteReporte.Alergia(alergia.getNombre(),
                severidad(alergia.getSeveridad())))
            .toList();
        List<ExpedientePacienteReporte.MenuAsignado> menus = asignaciones.stream()
            .map(asignacion -> new ExpedientePacienteReporte.MenuAsignado(asignacion.getMenu().getNombre(),
                asignacion.getMenu().getTipoDieta().getEtiqueta(), asignacion.getFechaInicio(), asignacion.getFechaFin(),
                asignacion.isActiva(), asignacion.isActiva() ? "Activa" : "Inactiva"))
            .toList();

        ExpedientePacienteReporte.Paciente ficha = new ExpedientePacienteReporte.Paciente(
            paciente.getCodigo(), nombreCompleto(paciente), paciente.calcularEdad(), paciente.getEstado().toString(),
            paciente.getPesoKg().toPlainString(), paciente.getAlturaCm().toPlainString(),
            piso(paciente.getCama()), cama(paciente.getCama()), patologias, medicamentos, alergias);
        String estadoFiltro = estadoAsignacion == null || estadoAsignacion.isBlank()
            ? "Todas" : "Activa".equalsIgnoreCase(estadoAsignacion) ? "Activas" : "Inactivas";
        List<String> filtros = new ArrayList<>();
        filtros.add("Código: " + paciente.getCodigo());
        filtros.add("Desde: " + (fechaDesde == null ? "Sin límite" : fechaDesde.format(FECHA)));
        filtros.add("Hasta: " + (fechaHasta == null ? "Sin límite" : fechaHasta.format(FECHA)));
        filtros.add("Asignaciones: " + estadoFiltro);
        EncabezadoReporte encabezado = encabezado("RD-01", "Expediente clínico-nutricional",
            "rd-01-expediente-" + paciente.getCodigo(), filtros,
            "Ficha vigente del paciente y menús asignados que cumplen los filtros de fecha y estado.");
        return new ExpedientePacienteReporte(encabezado, codigo, ficha, menus);
    }

    /** RD-03: distribución de pacientes ingresados por piso y régimen dietético prescrito. */
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO_PISO', 'MEDICO', 'NUTRICIONISTA', 'COCINA')")
    public HojaRutaEntregaReporte hojaRutaEntrega(Long pisoId, TipoDieta tipoDieta) {
        String pisoFiltro = filtroPiso(pisoId);
        Map<Long, List<String>> encargadosPorPiso = new LinkedHashMap<>();
        for (AsignacionPiso asignacion : reportes.buscarEncargadosPisos(pisoId)) {
            encargadosPorPiso.computeIfAbsent(asignacion.getPiso().getId(), clave -> new ArrayList<>())
                .add(asignacion.getUsuario().getNombreCompleto());
        }

        Map<Long, PrescripcionDieta> prescripcionesActuales = new LinkedHashMap<>();
        for (PrescripcionDieta prescripcion : reportes.buscarPrescripcionesActuales(
            EstadoPaciente.INGRESADO, EstadoPrescripcion.APROBADO, pisoId)) {
            prescripcionesActuales.putIfAbsent(prescripcion.getPaciente().getId(), prescripcion);
        }

        Map<Long, List<HojaRutaEntregaReporte.Fila>> filasPorPiso = new LinkedHashMap<>();
        Map<Long, String> pisosAgrupados = new LinkedHashMap<>();
        for (PrescripcionDieta prescripcion : prescripcionesActuales.values()) {
            Paciente paciente = prescripcion.getPaciente();
            if (paciente.getCama() == null || (tipoDieta != null && prescripcion.getTipoDieta() != tipoDieta)) {
                continue;
            }
            Piso piso = paciente.getCama().getPiso();
            pisosAgrupados.putIfAbsent(piso.getId(), etiqueta(piso));
            filasPorPiso.computeIfAbsent(piso.getId(), clave -> new ArrayList<>())
                .add(new HojaRutaEntregaReporte.Fila(cama(paciente.getCama()), paciente.getCodigo(),
                    nombreCompleto(paciente), prescripcion.getTipoDieta().getEtiqueta()));
        }

        List<HojaRutaEntregaReporte.GrupoPiso> grupos = pisosAgrupados.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(grupo -> {
                List<String> encargados = encargadosPorPiso.getOrDefault(grupo.getKey(), List.of());
                String nombreEncargado = encargados.isEmpty() ? "Sin encargado asignado" : String.join(", ",
                    encargados.stream().distinct().toList());
                return new HojaRutaEntregaReporte.GrupoPiso(grupo.getValue(), nombreEncargado,
                    List.copyOf(filasPorPiso.getOrDefault(grupo.getKey(), List.of())));
            })
            .toList();
        long total = grupos.stream().mapToLong(grupo -> grupo.filas().size()).sum();
        EncabezadoReporte encabezado = encabezado("RD-03", "Hoja de Ruta y Entrega",
            "rd-03-hoja-ruta-entrega-" + hoy(),
            List.of("Piso: " + pisoFiltro,
                "Tipo de dieta: " + (tipoDieta == null ? "Todas" : tipoDieta.getEtiqueta())),
            "Pacientes ingresados con la prescripción dietética aprobada más reciente. Solo se incluyen pacientes con cama asignada.");
        return new HojaRutaEntregaReporte(encabezado, grupos, total);
    }

    /** RD-04: historial de asignaciones de dieta y seguimiento de un paciente. */
    @PreAuthorize(CLINICOS)
    public HistorialDietasPacienteReporte historialDietasPaciente(String codigo, LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        Paciente paciente = reportes.buscarPacientePorCodigo(codigo)
            .orElseThrow(() -> new ResourceNotFoundException("No existe un paciente con el código indicado."));
        ZoneId zona = ZoneId.systemDefault();
        List<PrescripcionDieta> prescripciones = reportes.buscarHistorialPrescripciones(paciente.getId()).stream()
            .filter(prescripcion -> {
                LocalDate inicio = LocalDate.ofInstant(prescripcion.getFechaAutorizacion(), zona);
                return fechaHasta == null || !inicio.isAfter(fechaHasta);
            })
            .toList();
        List<HistorialDietasPacienteReporte.Fila> filas = prescripciones.stream().map(prescripcion -> {
            int indice = prescripciones.indexOf(prescripcion);
            LocalDate inicio = LocalDate.ofInstant(prescripcion.getFechaAutorizacion(), zona);
            LocalDate fin = indice == 0 ? null
                : LocalDate.ofInstant(prescripciones.get(indice - 1).getFechaAutorizacion(), zona).minusDays(1);
            boolean finalizada = indice > 0;
            LocalDate finDuracion = fin;
            if (finDuracion == null || finDuracion.isAfter(hoy())) {
                finDuracion = hoy();
            }
            if (finDuracion.isBefore(inicio)) {
                finDuracion = inicio;
            }
            long dias = ChronoUnit.DAYS.between(inicio, finDuracion) + 1;
            String autorizador = prescripcion.getAutorizadoPor().getNombreCompleto();
            return new HistorialDietasPacienteReporte.Fila(inicio, fin,
                prescripcion.getTipoDieta().getEtiqueta(), finalizada ? "Finalizada" : "Activa",
                dias, autorizador, autorizador, LocalDate.ofInstant(prescripcion.getFechaAutorizacion(), zona));
        })
            .filter(fila -> fechaDesde == null || fila.fin() == null || !fila.fin().isBefore(fechaDesde))
            .toList();

        PrescripcionDieta dietaActual = reportes.buscarPrescripcionesPaciente(
            paciente.getId(), EstadoPrescripcion.APROBADO).stream().findFirst().orElse(null);
        List<String> alergias = paciente.getAlergias().stream()
            .sorted(Comparator.comparing(alergia -> alergia.getNombre(), String.CASE_INSENSITIVE_ORDER))
            .map(alergia -> alergia.getNombre() + " (" + severidad(alergia.getSeveridad()) + ")")
            .toList();
        HistorialDietasPacienteReporte.Paciente ficha = new HistorialDietasPacienteReporte.Paciente(
            paciente.getCodigo(), nombreCompleto(paciente), paciente.calcularEdad(), paciente.getPesoKg().toPlainString(),
            piso(paciente.getCama()), cama(paciente.getCama()), alergias);
        List<String> filtros = new ArrayList<>();
        filtros.add("Código: " + paciente.getCodigo());
        filtros.add("Desde: " + (fechaDesde == null ? "Sin límite" : fechaDesde.format(FECHA)));
        filtros.add("Hasta: " + (fechaHasta == null ? "Sin límite" : fechaHasta.format(FECHA)));
        EncabezadoReporte encabezado = encabezado("RD-04", "Historial de dietas asignadas y seguimiento por paciente",
            "rd-04-historial-dietas-" + paciente.getCodigo(), filtros,
            "Asignaciones del paciente que se relacionan con el rango seleccionado.");
        return new HistorialDietasPacienteReporte(encabezado, paciente.getCodigo(), ficha, filas, filas.size(),
            dietaActual == null ? "Sin prescripción activa vigente" : dietaActual.getTipoDieta().getEtiqueta());
    }

    /** RD-02: gramajes, insumos y costo por ración de un menú. */
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'COCINA')")
    public HojaTecnicaMenuReporte hojaTecnicaMenu(Long menuId) {
        Menu menu = menus.findById(menuId)
            .orElseThrow(() -> new ResourceNotFoundException("El menú solicitado no existe."));
        List<HojaTecnicaMenuReporte.Fila> filas = menu.getDetalles().stream()
            .flatMap(detalle -> detalle.getInsumos().stream()
                .map(detalleInsumo -> {
                    BigDecimal costoUnitario = detalleInsumo.getInsumo().getCostoUnitario();
                    if (costoUnitario == null) {
                        costoUnitario = BigDecimal.ZERO;
                    }
                    costoUnitario = costoUnitario.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal costo = detalleInsumo.getCantidadPorRacion().multiply(costoUnitario)
                        .setScale(2, RoundingMode.HALF_UP);
                    return new HojaTecnicaMenuReporte.Fila(
                        detalle.getTiempoComida().getEtiqueta(),
                        detalle.getHoraServicio(),
                        detalle.getPlatillo(),
                        detalleInsumo.getInsumo().getNombre(),
                        detalleInsumo.getCantidadPorRacion(),
                        detalleInsumo.getInsumo().getUnidad(),
                        costoUnitario,
                        costo);
                }))
            .toList();
        BigDecimal costoTotal = filas.stream()
            .map(HojaTecnicaMenuReporte.Fila::costo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, List<HojaTecnicaMenuReporte.Fila>> filasPorComida = new LinkedHashMap<>();
        filas.forEach(fila -> filasPorComida.computeIfAbsent(fila.comida(), comida -> new ArrayList<>()).add(fila));
        List<HojaTecnicaMenuReporte.GrupoComida> comidas = filasPorComida.entrySet().stream()
            .map(grupo -> {
                BigDecimal subtotal = grupo.getValue().stream()
                    .map(HojaTecnicaMenuReporte.Fila::costo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
                return new HojaTecnicaMenuReporte.GrupoComida(grupo.getKey(), grupo.getValue(), subtotal);
            })
            .toList();
        BigDecimal costoUnitarioRedondeado = costoTotal.setScale(2, RoundingMode.HALF_UP);
        EncabezadoReporte encabezado = encabezado("RD-02", "Hoja técnica de gramajes por ración",
            "rd-02-hoja-tecnica-" + menu.getId(), List.of("Menú: " + menu.getNombre()),
            "Cantidad por ración y costo calculado a partir del costo unitario de cada insumo.");
        return new HojaTecnicaMenuReporte(encabezado, menu.getNombre(), menu.getTipoDieta().getEtiqueta(),
            comidas, filas, costoUnitarioRedondeado);
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

    private static String severidad(SeveridadAlergia severidad) {
        return switch (severidad) {
            case LEVE -> "Leve";
            case MODERADA -> "Moderada";
            case SEVERA -> "Grave";
        };
    }

    private static final java.time.format.DateTimeFormatter FECHA_HORA =
        java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
}
