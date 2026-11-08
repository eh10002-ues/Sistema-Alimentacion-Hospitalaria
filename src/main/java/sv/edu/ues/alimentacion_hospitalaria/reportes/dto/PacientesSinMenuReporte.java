package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * RE-04 (propuesta de sustitución del reporte de interacciones fármaco-nutriente, pendiente de validación):
 * pacientes ingresados sin asignación de menú activa, aprobada y vigente en la fecha.
 */
public record PacientesSinMenuReporte(EncabezadoReporte encabezado, LocalDate fecha, List<Fila> filas,
                                      long conAsignacionPendiente) {

    /** {@code ultimoMenu} y sus fechas son nulos si el paciente nunca tuvo una asignación aprobada. */
    public record Fila(String piso, String cama, String codigo, String paciente, boolean asignacionPendiente,
                       String ultimoMenu, LocalDate ultimoMenuInicio, LocalDate ultimoMenuFin) {
    }
}
