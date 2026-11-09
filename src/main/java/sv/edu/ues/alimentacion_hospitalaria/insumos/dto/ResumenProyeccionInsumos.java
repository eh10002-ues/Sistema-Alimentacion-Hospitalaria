package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenProyeccionInsumos(List<TotalDemanda> demanda, List<TotalCompra> compras,
                                       BigDecimal presupuestoTotal) {

    public record TotalDemanda(String categoria, BigDecimal cantidad, String unidad) {
    }

    public record TotalCompra(String insumo, String presentacion, BigDecimal contenido, String unidad,
                              BigDecimal cantidad, BigDecimal precioPresentacion, BigDecimal subtotal) {
    }
}
