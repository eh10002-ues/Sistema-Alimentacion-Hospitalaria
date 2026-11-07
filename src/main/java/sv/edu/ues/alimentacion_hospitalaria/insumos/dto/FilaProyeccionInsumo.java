package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FilaProyeccionInsumo(
    Long insumoId,
    Long asignacionId,
    Long pacienteId,
    String nombre,
    String unidad,
    BigDecimal cantidadPorRacion,
    BigDecimal merma,
    String presentacion,
    BigDecimal cantidadPresentacion,
    BigDecimal costoUnitario,
    LocalDate fechaInicio,
    LocalDate fechaFin
) {
}
