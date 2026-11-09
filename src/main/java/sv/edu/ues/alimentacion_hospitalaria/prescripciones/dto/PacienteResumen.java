package sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto;

import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;

/** Fila del listado de pacientes ingresados que pueden recibir una prescripción. */
public record PacienteResumen(Long id, String codigo, String nombreCompleto, String ubicacion) {

    public static PacienteResumen desde(Paciente paciente) {
        return new PacienteResumen(paciente.getId(), paciente.getCodigo(),
            nombreCompleto(paciente), ubicacion(paciente));
    }

    static String nombreCompleto(Paciente paciente) {
        return paciente.getNombres() + " " + paciente.getApellidos();
    }

    static String ubicacion(Paciente paciente) {
        if (paciente.getCama() == null) {
            return "Sin cama asignada";
        }
        return paciente.getCama().getPiso().getNombre() + " · Cama " + paciente.getCama().getNumero();
    }
}
