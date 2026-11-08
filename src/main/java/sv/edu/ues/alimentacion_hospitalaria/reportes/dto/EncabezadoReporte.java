package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.Instant;
import java.util.List;

/**
 * Datos comunes de los reportes de SAH-9, compartidos por la pantalla y el PDF.
 * {@code archivo} es el nombre del PDF sin extensión; {@code nota} aclara el alcance del reporte.
 */
public record EncabezadoReporte(String codigo, String titulo, String archivo, List<String> filtros, String nota,
                                String generadoPor, Instant generadoEn) {
}
