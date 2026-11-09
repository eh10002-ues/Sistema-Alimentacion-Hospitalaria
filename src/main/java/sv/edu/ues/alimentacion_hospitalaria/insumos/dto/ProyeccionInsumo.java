package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProyeccionInsumo(
    Long id,
    String nombre,
    String unidad,
    BigDecimal neto,
    BigDecimal bruto,
    BigDecimal totalConMargen,
    String presentacion,
    BigDecimal cantidadPresentacion,
    BigDecimal cantidadComprar,
    BigDecimal costo,
    BigDecimal precioPresentacion,
    BigDecimal presupuestoCompra,
    List<String> advertencias
) {
}
