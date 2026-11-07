package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProyeccionInsumos(
    PeriodoProyeccion periodo,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    BigDecimal margenPorcentaje,
    List<ProyeccionInsumo> insumos,
    BigDecimal costoTotal,
    int pacientesIncluidos,
    BigDecimal racionesPorDia,
    List<String> advertencias
) {
}
