package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record RacionesServidasVista(YearMonth periodo, LocalDate fechaCorte, List<TotalPiso> totalesPorPiso,
                                    List<Fila> filas, long totalRaciones) {

    public record TotalPiso(String piso, long raciones) {
    }

    public record Fila(String piso, String menu, String servicio, String plato, long raciones) {
    }
}
