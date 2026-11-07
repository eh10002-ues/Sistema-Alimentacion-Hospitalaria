package sv.edu.ues.alimentacion_hospitalaria.pacientes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;

public record PacienteDetalleVista(
    Long id,
    String codigo,
    String nombres,
    String apellidos,
    LocalDate fechaNacimiento,
    int edad,
    BigDecimal pesoKg,
    BigDecimal alturaCm,
    EstadoPaciente estado,
    String ubicacion,
    List<String> enfermedades,
    List<AlergiaVista> alergias,
    List<MedicamentoVista> medicamentos) {

    public static PacienteDetalleVista desde(Paciente paciente) {
        String ubicacion = paciente.getCama() == null
            ? "Sin ubicación"
            : paciente.getCama().getPiso().getCodigo() + " - " + paciente.getCama().getNumero();
        return new PacienteDetalleVista(
            paciente.getId(),
            paciente.getCodigo(),
            paciente.getNombres(),
            paciente.getApellidos(),
            paciente.getFechaNacimiento(),
            paciente.calcularEdad(),
            paciente.getPesoKg(),
            paciente.getAlturaCm(),
            paciente.getEstado(),
            ubicacion,
            paciente.getEnfermedades().stream().map(enfermedad -> enfermedad.getNombre()).toList(),
            paciente.getAlergias().stream()
                .map(alergia -> new AlergiaVista(alergia.getNombre(), alergia.getSeveridad()))
                .toList(),
            paciente.getMedicamentos().stream()
                .map(medicamento -> new MedicamentoVista(
                    medicamento.getNombre(), medicamento.getDosis(), medicamento.getFrecuenciaHoras(),
                    medicamento.getHoraPrimeraDosis()))
                .toList());
    }

    public record AlergiaVista(String nombre, SeveridadAlergia severidad) { }

    public record MedicamentoVista(String nombre, String dosis, short frecuenciaHoras,
                                   LocalTime horaPrimeraDosis) { }
}
