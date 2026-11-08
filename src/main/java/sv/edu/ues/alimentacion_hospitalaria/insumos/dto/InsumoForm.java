package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;

@Getter
@Setter
public class InsumoForm {

    @NotBlank(message = "Ingrese el nombre del insumo.")
    @Size(max = 150, message = "El nombre admite hasta 150 caracteres.")
    private String nombre;

    @NotBlank(message = "Ingrese la unidad base del insumo.")
    @Size(max = 10, message = "La unidad admite hasta 10 caracteres.")
    private String unidad;

    @NotNull(message = "Ingrese la merma; use 0 si no aplica.")
    @DecimalMin(value = "0.00", message = "La merma no puede ser negativa.")
    @DecimalMax(value = "99.99", message = "La merma debe ser menor que 100%.")
    @Digits(integer = 2, fraction = 2, message = "Use hasta dos decimales para la merma.")
    private BigDecimal mermaPorcentaje = BigDecimal.ZERO;

    @Size(max = 50, message = "La presentación admite hasta 50 caracteres.")
    private String presentacion;

    @DecimalMin(value = "0.01", message = "La cantidad por presentación debe ser mayor que cero.")
    @Digits(integer = 8, fraction = 2, message = "La cantidad admite hasta dos decimales.")
    private BigDecimal cantidadPresentacion;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo.")
    @Digits(integer = 6, fraction = 4, message = "El costo unitario admite hasta cuatro decimales.")
    private BigDecimal costoUnitario;

    public static InsumoForm desde(Insumo insumo) {
        InsumoForm formulario = new InsumoForm();
        formulario.setNombre(insumo.getNombre());
        formulario.setUnidad(insumo.getUnidad());
        formulario.setMermaPorcentaje(insumo.getMerma() == null
            ? BigDecimal.ZERO : insumo.getMerma().movePointRight(2));
        formulario.setPresentacion(insumo.getPresentacion());
        formulario.setCantidadPresentacion(insumo.getCantidadPresentacion());
        formulario.setCostoUnitario(insumo.getCostoUnitario());
        return formulario;
    }

    public boolean tienePresentacionCompleta() {
        boolean tieneNombre = presentacion != null && !presentacion.isBlank();
        return tieneNombre == (cantidadPresentacion != null);
    }
}
