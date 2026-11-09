package sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto;

import java.math.BigDecimal;
import java.util.List;

import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;

/** Datos de solo lectura que el médico o nutricionista revisa antes de autorizar la dieta. */
public record EvaluacionPaciente(Long id, String codigo, String nombreCompleto, int edad, BigDecimal pesoKg,
                                 BigDecimal alturaCm, String ubicacion, List<String> enfermedades,
                                 List<String> alergias, List<String> medicamentos,
                                 List<PrescripcionVista> historial) {

    public static EvaluacionPaciente desde(Paciente paciente, List<PrescripcionVista> historial) {
        return new EvaluacionPaciente(paciente.getId(), paciente.getCodigo(),
            PacienteResumen.nombreCompleto(paciente), paciente.calcularEdad(), paciente.getPesoKg(),
            paciente.getAlturaCm(), PacienteResumen.ubicacion(paciente),
            paciente.getEnfermedades().stream().map(enfermedad -> enfermedad.getNombre()).toList(),
            paciente.getAlergias().stream()
                .map(alergia -> alergia.getNombre() + " (" + alergia.getSeveridad().name().toLowerCase() + ")").toList(),
            paciente.getMedicamentos().stream()
                .map(medicamento -> medicamento.getNombre() + " " + medicamento.getDosis()
                    + " cada " + medicamento.getFrecuenciaHoras() + " h").toList(),
            historial);
    }
}
