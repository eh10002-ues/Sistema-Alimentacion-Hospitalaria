package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.LocalDate;
import java.util.List;

public record ExpedientePacienteReporte(EncabezadoReporte encabezado, String codigoBuscado,
                                        Paciente paciente, List<MenuAsignado> menusAsignados) {

    public record Paciente(String codigo, String nombre, int edad, String estado, String pesoKg,
                           String alturaCm, String piso, String cama, List<String> patologias,
                           List<Medicamento> medicamentos, List<Alergia> alergias) {
    }

    public record Medicamento(String nombre, String dosis, short frecuenciaHoras, String horaPrimeraDosis) {
    }

    public record Alergia(String nombre, String severidad) {
    }

    public record MenuAsignado(String nombre, String tipoDieta, LocalDate fechaInicio, LocalDate fechaFin,
                               boolean activa, String estado) {
    }
}
