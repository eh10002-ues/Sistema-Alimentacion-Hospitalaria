package sv.edu.ues.alimentacion_hospitalaria.menus.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Insumo y gramaje por ración de un tiempo de comida. */
@Getter
@Setter
public class InsumoDetalleForm {

    @NotNull(message = "Seleccione el insumo.")
    private Long insumoId;

    @NotNull(message = "Ingrese la cantidad por ración.")
    @DecimalMin(value = "0.01", message = "La cantidad por ración debe ser mayor que cero.")
    @Digits(integer = 8, fraction = 2, message = "La cantidad admite hasta dos decimales.")
    private BigDecimal cantidadPorRacion;
}
