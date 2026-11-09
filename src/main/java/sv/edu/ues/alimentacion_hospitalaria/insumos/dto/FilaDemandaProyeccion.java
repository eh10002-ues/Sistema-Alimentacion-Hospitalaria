package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import java.time.LocalDate;

public record FilaDemandaProyeccion(
    Long asignacionId,
    Long pacienteId,
    LocalDate fechaInicio,
    LocalDate fechaFin
) {
}
