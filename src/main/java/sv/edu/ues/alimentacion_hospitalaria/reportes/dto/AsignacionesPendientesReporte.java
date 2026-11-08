package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** RE-03: asignaciones de menú pendientes de aprobación. No son prescripciones dietéticas. */
public record AsignacionesPendientesReporte(EncabezadoReporte encabezado, List<Fila> filas) {

    public record Fila(String codigo, String paciente, String piso, String cama, String menu, String regimen,
                       LocalDate fechaInicio, LocalDate fechaFin, String asignadoPor, Instant solicitadaEl) {
    }
}
