package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.util.List;

/** RE-01: pacientes ingresados con alergias severas registradas, agrupados por piso. */
public record AlergiasSeverasReporte(EncabezadoReporte encabezado, List<GrupoPiso> pisos, int totalPacientes,
                                     int totalAlergias) {

    public record GrupoPiso(String piso, List<Fila> filas) {
    }

    public record Fila(String piso, String cama, String codigo, String paciente, String alergia, String severidad) {
    }
}
