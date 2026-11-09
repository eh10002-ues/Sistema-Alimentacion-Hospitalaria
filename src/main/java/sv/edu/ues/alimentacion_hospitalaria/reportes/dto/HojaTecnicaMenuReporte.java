package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record HojaTecnicaMenuReporte(EncabezadoReporte encabezado, String menu, String tipoDieta,
                                     List<GrupoComida> comidas, List<Fila> filas, BigDecimal costoTotal) {

    public record Fila(String comida, LocalTime hora, String platillo, String insumo, BigDecimal cantidad,
                       String unidad, BigDecimal costoUnitario, BigDecimal costo) {
    }

    public record GrupoComida(String comida, List<Fila> filas, BigDecimal costoTotal) {
    }
}
