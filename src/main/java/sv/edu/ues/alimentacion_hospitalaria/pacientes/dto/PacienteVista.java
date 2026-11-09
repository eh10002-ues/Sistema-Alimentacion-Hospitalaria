package sv.edu.ues.alimentacion_hospitalaria.pacientes.dto;

import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;

public record PacienteVista(
    Long id,
    String codigo,
    String nombres,
    String apellidos,
    Integer enfermedades,
    Integer alergias,
    String ubicacion,
    EstadoPaciente estado) {

    public static PacienteVista desde(Paciente paciente) {
        return desde(paciente, true);
    }

    public static PacienteVista desde(Paciente paciente, boolean incluirClinica) {
        String ubicacion = paciente.getCama() == null
            ? "Sin ubicación"
            : paciente.getCama().getPiso().getCodigo() + " - " + paciente.getCama().getNumero();
        return new PacienteVista(
            paciente.getId(),
            paciente.getCodigo(),
            paciente.getNombres(),
            paciente.getApellidos(),
            incluirClinica ? paciente.getEnfermedades().size() : null,
            incluirClinica ? paciente.getAlergias().size() : null,
            ubicacion,
            paciente.getEstado());
    }
}
