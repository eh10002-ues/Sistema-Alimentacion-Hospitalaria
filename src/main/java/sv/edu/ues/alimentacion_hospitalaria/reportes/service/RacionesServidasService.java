package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.RacionesServidasVista;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.RacionesServidasVista.Fila;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.RacionesServidasVista.TotalPiso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO', 'COCINA')")
public class RacionesServidasService {

    private final AsignacionMenuRepository asignaciones;

    public RacionesServidasService(AsignacionMenuRepository asignaciones) {
        this.asignaciones = asignaciones;
    }

    public RacionesServidasVista generar(YearMonth periodo, LocalDate hoy) {
        if (periodo == null || hoy == null) {
            throw new IllegalArgumentException("Seleccione el mes del reporte.");
        }
        if (periodo.isAfter(YearMonth.from(hoy))) {
            throw new IllegalArgumentException("El reporte no admite meses futuros.");
        }

        LocalDate desde = periodo.atDay(1);
        LocalDate fechaCorte = periodo.atEndOfMonth().isAfter(hoy) ? hoy : periodo.atEndOfMonth();
        Map<ClaveFila, Long> cantidades = new TreeMap<>(Comparator
            .comparing(ClaveFila::piso)
            .thenComparing(ClaveFila::servicio)
            .thenComparing(ClaveFila::plato)
            .thenComparing(ClaveFila::menu));

        for (AsignacionMenu asignacion : asignaciones.findAprobadasConTraslape(
            desde, fechaCorte, EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO)) {
            LocalDate inicioServicio = asignacion.getFechaInicio().isAfter(desde)
                ? asignacion.getFechaInicio() : desde;
            LocalDate finAsignacion = asignacion.getFechaFin();
            LocalDate finServicio = finAsignacion != null && finAsignacion.isBefore(fechaCorte)
                ? finAsignacion : fechaCorte;
            if (inicioServicio.isAfter(finServicio)) {
                continue;
            }

            long diasServidos = ChronoUnit.DAYS.between(inicioServicio, finServicio) + 1;
            Paciente paciente = asignacion.getPaciente();
            Cama cama = paciente.getCama();
            String piso = cama == null ? "Sin ubicación"
                : cama.getPiso().getCodigo() + " — " + cama.getPiso().getNombre();

            for (MenuDetalle detalle : asignacion.getMenu().getDetalles()) {
                ClaveFila clave = new ClaveFila(piso, asignacion.getMenu().getNombre(),
                    detalle.getTiempoComida().getEtiqueta(), detalle.getPlatillo());
                cantidades.merge(clave, diasServidos, Long::sum);
            }
        }

        List<Fila> filas = cantidades.entrySet().stream()
            .map(entrada -> new Fila(entrada.getKey().piso(), entrada.getKey().menu(),
                entrada.getKey().servicio(), entrada.getKey().plato(), entrada.getValue()))
            .toList();
        Map<String, Long> totalesPorPiso = new TreeMap<>();
        cantidades.forEach((clave, cantidad) -> totalesPorPiso.merge(clave.piso(), cantidad, Long::sum));
        List<TotalPiso> pisos = totalesPorPiso.entrySet().stream()
            .map(entrada -> new TotalPiso(entrada.getKey(), entrada.getValue()))
            .toList();
        long totalRaciones = cantidades.values().stream().mapToLong(Long::longValue).sum();
        return new RacionesServidasVista(periodo, fechaCorte, pisos, filas, totalRaciones);
    }

    private record ClaveFila(String piso, String menu, String servicio, String plato) {
    }
}
