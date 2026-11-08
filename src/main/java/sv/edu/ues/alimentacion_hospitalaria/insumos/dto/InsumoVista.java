package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import java.math.BigDecimal;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;

public record InsumoVista(
    Long id,
    String nombre,
    String unidad,
    BigDecimal mermaPorcentaje,
    String presentacion,
    BigDecimal cantidadPresentacion,
    BigDecimal costoUnitario,
    boolean enUso
) {
    public static InsumoVista desde(Insumo insumo, boolean enUso) {
        BigDecimal mermaPorcentaje = insumo.getMerma() == null
            ? BigDecimal.ZERO : insumo.getMerma().movePointRight(2);
        return new InsumoVista(
            insumo.getId(),
            insumo.getNombre(),
            insumo.getUnidad(),
            mermaPorcentaje,
            insumo.getPresentacion(),
            insumo.getCantidadPresentacion(),
            insumo.getCostoUnitario(),
            enUso);
    }
}
