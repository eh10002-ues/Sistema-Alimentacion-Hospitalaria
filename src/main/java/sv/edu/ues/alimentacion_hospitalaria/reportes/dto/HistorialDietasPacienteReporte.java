package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.LocalDate;
import java.util.List;

public record HistorialDietasPacienteReporte(EncabezadoReporte encabezado, String codigoPaciente,
                                             Paciente paciente, List<Fila> asignaciones,
                                             long totalAsignaciones, String dietaActual) {

    public record Paciente(String codigo, String nombre, int edad, String pesoKg, String piso, String cama,
                           List<String> alergias) {
    }

    public record Fila(LocalDate inicio, LocalDate fin, String tipoDieta, String estado, long diasDuracion,
                       String asignadoPor, String aprobadoPor, LocalDate fechaAprobacion) {
    }
}
