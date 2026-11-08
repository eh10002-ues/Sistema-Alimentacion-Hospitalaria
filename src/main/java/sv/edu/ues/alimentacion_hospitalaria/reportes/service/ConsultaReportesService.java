package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

import jakarta.persistence.Tuple;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.data.domain.PageRequest;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilasReporte.*;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte.EstadoOcupacion;
import sv.edu.ues.alimentacion_hospitalaria.reportes.persistence.ConsultasReportesRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

/** HU-02 paso 3: consultas paginadas y transformación segura por rol. */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO', 'COCINA')")
public class ConsultaReportesService {
    public static final int LIMITE_IMPRESION = 1000;
    private static final ZoneId ZONA = ZoneId.of("America/El_Salvador");
    private final ConsultasReportesRepository consultas;
    private final ValidacionConsultaReportes validacion;
    private final AccesoReportes acceso;

    public ConsultaReportesService(ConsultasReportesRepository consultas, ValidacionConsultaReportes validacion,
                                   AccesoReportes acceso) {
        this.consultas = consultas;
        this.validacion = validacion;
        this.acceso = acceso;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO')")
    public Page<FilasReporte.Paciente> pacientes(FiltrosReporte.Pacientes filtro) {
        return pacientes(filtro, false);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<FilasReporte.Paciente> pacientesImpresion(FiltrosReporte.Pacientes filtro) {
        return pacientes(filtro, true);
    }

    private Page<FilasReporte.Paciente> pacientes(FiltrosReporte.Pacientes filtro, boolean impresion) {
        acceso.rolActual();
        validacion.validar(filtro, filtro == null ? null : filtro.pisoId());
        return consultas.pacientes(filtro, paginacion(filtro.pagina(), impresion), impresion ? LIMITE_IMPRESION : null).map(fila -> {
            EstadoPaciente estado = fila.get(4, EstadoPaciente.class);
            return new FilasReporte.Paciente(fila.get(0, String.class), fila.get(1, String.class),
                fila.get(2, String.class), fila.get(3, String.class), estado,
                estado == EstadoPaciente.ALTA && fila.get(3) != null);
        });
    }

    public ResultadoFechado<Asignacion> asignaciones(FiltrosReporte.Asignaciones filtro) {
        return asignaciones(filtro, false);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ResultadoFechado<Asignacion> asignacionesImpresion(FiltrosReporte.Asignaciones filtro) {
        return asignaciones(filtro, true);
    }

    private ResultadoFechado<Asignacion> asignaciones(FiltrosReporte.Asignaciones filtro, boolean impresion) {
        var rol = acceso.rolActual();
        validacion.validar(filtro, filtro == null ? null : filtro.pisoId());
        LocalDate fecha = fechaConsulta(filtro.fecha());
        Page<Tuple> pagina = consultas.asignaciones(filtro, fecha, paginacion(filtro.pagina(), impresion), impresion ? LIMITE_IMPRESION : null);
        Map<Long, List<Tiempo>> tiempos = new HashMap<>();
        // Piso no recibe ni necesita tiempos de comida.
        if (rol != Rol.ROLE_ENCARGADO_PISO) {
            var menus = pagina.stream().map(fila -> fila.get(10, Long.class)).distinct().toList();
            for (Tuple fila : consultas.tiempos(menus)) {
                tiempos.computeIfAbsent(fila.get(0, Long.class), id -> new java.util.ArrayList<>())
                    .add(new Tiempo(fila.get(1, TiempoComida.class), fila.get(2, LocalTime.class)));
            }
            tiempos.values().forEach(lista -> lista.sort(Comparator.comparing(Tiempo::horaServicio)
                .thenComparing(Tiempo::tiempoComida)));
        }
        Page<Asignacion> resultado = pagina.map(fila -> {
            var paciente = new PacienteUbicacion(fila.get(1, String.class), fila.get(2, String.class),
                fila.get(3, String.class), fila.get(4, String.class));
            var dieta = fila.get(5, TipoDieta.class);
            String menu = fila.get(6, String.class);
            List<Tiempo> comidas = tiempos.getOrDefault(fila.get(10, Long.class), List.of());
            return switch (rol) {
                case ROLE_ENCARGADO_PISO -> new AsignacionPiso(paciente, dieta, menu, "Vigente");
                case ROLE_COCINA -> new AsignacionCocina(paciente, dieta, menu, fecha, comidas);
                default -> new AsignacionClinica(paciente, dieta, menu, fila.get(7, LocalDate.class),
                    fila.get(8, LocalDate.class), "Vigente", fila.get(9, String.class), comidas);
            };
        });
        return new ResultadoFechado<>(fecha, resultado);
    }

    public ResultadoFechado<FilasReporte.Menu> menus(FiltrosReporte.Menus filtro) {
        return menus(filtro, false);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ResultadoFechado<FilasReporte.Menu> menusImpresion(FiltrosReporte.Menus filtro) {
        return menus(filtro, true);
    }

    private ResultadoFechado<FilasReporte.Menu> menus(FiltrosReporte.Menus filtro, boolean impresion) {
        acceso.rolActual();
        validacion.validar(filtro, null);
        LocalDate fecha = fechaConsulta(filtro.fecha());
        var pagina = consultas.menus(filtro, fecha, paginacion(filtro.pagina(), impresion), impresion ? LIMITE_IMPRESION : null).map(fila ->
            new FilasReporte.Menu(fecha, fila.get(0, TiempoComida.class), fila.get(1, LocalTime.class),
                fila.get(2, String.class), fila.get(3, TipoDieta.class), fila.get(4, EstadoMenu.class)));
        return new ResultadoFechado<>(fecha, pagina);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO')")
    public Page<Ocupacion> ocupacion(FiltrosReporte.Ocupacion filtro) {
        return ocupacion(filtro, false);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<Ocupacion> ocupacionImpresion(FiltrosReporte.Ocupacion filtro) {
        return ocupacion(filtro, true);
    }

    private Page<Ocupacion> ocupacion(FiltrosReporte.Ocupacion filtro, boolean impresion) {
        acceso.rolActual();
        validacion.validar(filtro, filtro == null ? null : filtro.pisoId());
        Page<Tuple> pagina = consultas.ocupacion(filtro, paginacion(filtro.pagina(), impresion), impresion ? LIMITE_IMPRESION : null);
        var camas = pagina.stream().map(fila -> fila.get(0, Long.class)).toList();
        Map<Long, List<Ocupante>> ocupantes = new HashMap<>();
        for (Tuple fila : consultas.ocupantes(camas)) {
            ocupantes.computeIfAbsent(fila.get(0, Long.class), id -> new java.util.ArrayList<>())
                .add(new Ocupante(fila.get(1, String.class), fila.get(2, String.class)));
        }
        return pagina.map(fila -> {
            long cantidad = fila.get(3, Long.class);
            EstadoOcupacion estado = cantidad == 0 ? EstadoOcupacion.DISPONIBLE
                : cantidad == 1 ? EstadoOcupacion.OCUPADA : EstadoOcupacion.INCONSISTENTE;
            return new Ocupacion(fila.get(1, String.class), fila.get(2, String.class), estado,
                ocupantes.getOrDefault(fila.get(0, Long.class), List.of()));
        });
    }

    private PageRequest paginacion(int pagina, boolean impresion) {
        return impresion ? PageRequest.of(0, LIMITE_IMPRESION) : validacion.pagina(pagina);
    }

    private LocalDate fechaConsulta(LocalDate fecha) {
        return fecha == null ? LocalDate.now(ZONA) : fecha;
    }
}
