package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ProyeccionInsumosForm {

    @NotNull
    private PeriodoProyeccion periodo = PeriodoProyeccion.SEMANAL;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("1.00")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal margenFraccion = new BigDecimal("0.10");

    public PeriodoProyeccion getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoProyeccion periodo) {
        this.periodo = periodo;
    }

    public BigDecimal getMargenFraccion() {
        return margenFraccion;
    }

    public void setMargenFraccion(BigDecimal margenFraccion) {
        this.margenFraccion = margenFraccion;
    }

    public BigDecimal getMargenPorcentaje() {
        return margenFraccion == null ? null : margenFraccion.multiply(new BigDecimal("100"));
    }
}
