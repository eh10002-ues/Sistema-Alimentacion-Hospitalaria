package sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto;

import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;

/**
 * Datos que recibe cocina al aprobarse una prescripción. Solo incluye lo necesario
 * para preparar y entregar la dieta; no expone diagnósticos ni medicamentos.
 * La fecha viaja en formato ISO-8601 (UTC) y el navegador la muestra en hora local.
 */
public record OrdenCocina(Long prescripcionId, String codigoPaciente, String paciente, String ubicacion,
                          String regimen, String estado, String fechaAutorizacion) {

    public static OrdenCocina desde(PrescripcionDieta prescripcion) {
        var paciente = prescripcion.getPaciente();
        return new OrdenCocina(prescripcion.getId(), paciente.getCodigo(), PacienteResumen.nombreCompleto(paciente),
            PacienteResumen.ubicacion(paciente), prescripcion.getRegimen().getEtiqueta(),
            prescripcion.getEstado().getEtiqueta(), prescripcion.getFechaAutorizacion().toString());
    }
}
