package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.util.List;

public record HojaRutaEntregaReporte(EncabezadoReporte encabezado, List<GrupoPiso> pisos, long totalPacientes) {

    public record GrupoPiso(String piso, String encargado, List<Fila> filas) {
    }

    public record Fila(String cama, String codigo, String paciente, String dieta) {
    }
}
