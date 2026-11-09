package sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto;

import java.time.LocalDate;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;

/** Modelos de vista de las asignaciones de menú (no son prescripciones dietéticas de SAH-2). */
public final class VistasAsignacion {

    private VistasAsignacion() {
    }

    public record Fila(Long id, String codigo, String paciente, String ubicacion, String menu, TipoDieta tipoDieta,
                       LocalDate fechaInicio, LocalDate fechaFin, String asignadoPor, String aprobadoPor,
                       LocalDate fechaAprobacion) {

        public boolean pendiente() {
            return aprobadoPor == null;
        }
    }

    public record Opcion(Long id, String etiqueta) { }
}
