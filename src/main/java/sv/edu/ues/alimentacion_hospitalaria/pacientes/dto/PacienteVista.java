package sv.edu.ues.alimentacion_hospitalaria.pacientes.dto;

import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;

public record PacienteVista(
    Long id,
    String codigo,
    String nombres,
    String apellidos,
    int enfermedades,
    int alergias,
    String ubicacion,
    EstadoPaciente estado) {

    public static PacienteVista desde(Paciente paciente) {
        String ubicacion = paciente.getCama() == null
            ? "Sin ubicación"
            : paciente.getCama().getPiso().getCodigo() + " - " + paciente.getCama().getNumero();
        return new PacienteVista(
            paciente.getId(),
            paciente.getCodigo(),
            paciente.getNombres(),
            paciente.getApellidos(),
            paciente.getEnfermedades().size(),
            paciente.getAlergias().size(),
            ubicacion,
            paciente.getEstado());
    }
}
