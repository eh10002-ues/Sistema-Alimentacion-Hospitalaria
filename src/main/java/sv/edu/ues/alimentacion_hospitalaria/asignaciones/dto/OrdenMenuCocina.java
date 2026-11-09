package sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto;

import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto.PacienteResumen;

/**
 * Menú aprobado que cocina debe preparar para un paciente. Es independiente de las
 * prescripciones de SAH-2: proviene de una asignación de menú (D4) aprobada y vigente.
 * Solo incluye datos operativos; las fechas viajan en ISO-8601.
 */
public record OrdenMenuCocina(Long asignacionId, String codigoPaciente, String paciente, String ubicacion,
                              String menu, String regimen, String fechaInicio, String fechaFin) {

    public static OrdenMenuCocina desde(AsignacionMenu asignacion) {
        PacienteResumen paciente = PacienteResumen.desde(asignacion.getPaciente());
        return new OrdenMenuCocina(asignacion.getId(), paciente.codigo(), paciente.nombreCompleto(),
            paciente.ubicacion(), asignacion.getMenu().getNombre(), asignacion.getMenu().getTipoDieta().getEtiqueta(),
            asignacion.getFechaInicio().toString(),
            asignacion.getFechaFin() == null ? null : asignacion.getFechaFin().toString());
    }
}
