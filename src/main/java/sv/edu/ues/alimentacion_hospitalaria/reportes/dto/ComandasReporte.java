package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * RE-02: raciones por tiempo de comida, con subtotal por tiempo y total general.
 * No incluye horas de servicio ni datos clínicos.
 */
public record ComandasReporte(EncabezadoReporte encabezado, LocalDate fecha, List<GrupoTiempo> tiempos,
                              long totalGeneral) {

    public record GrupoTiempo(String tiempoComida, List<Fila> filas, long subtotal) {
    }

    public record Fila(String platillo, String menu, String regimen, String piso, long raciones) {
    }
}
