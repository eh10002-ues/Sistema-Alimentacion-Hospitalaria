package sv.edu.ues.alimentacion_hospitalaria.reportes.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.TipoReporte;

/** Conserva solo los filtros reconocidos y codifica sus valores como parámetros de formulario. */
@Component
public class EnlacesConsultaReportes {
    public record Enlaces(String anterior, String siguiente, String primera, String impresion) { }

    public Enlaces crear(TipoReporte tipo, Object filtros, LocalDate fecha, Page<?> pagina) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        switch (filtros) {
            case FiltrosReporte.Pacientes f -> {
                parametros.put("buscar", f.buscar()); parametros.put("pisoId", f.pisoId()); parametros.put("estado", f.estado());
            }
            case FiltrosReporte.Asignaciones f -> {
                parametros.put("buscar", f.buscar()); parametros.put("pisoId", f.pisoId());
                parametros.put("fecha", fecha); parametros.put("tipoDieta", f.tipoDieta());
            }
            case FiltrosReporte.Menus f -> {
                parametros.put("fecha", fecha); parametros.put("tipoDieta", f.tipoDieta());
                parametros.put("tiempoComida", f.tiempoComida());
            }
            case FiltrosReporte.Ocupacion f -> {
                parametros.put("pisoId", f.pisoId()); parametros.put("cama", f.cama()); parametros.put("ocupacion", f.ocupacion());
            }
            default -> throw new IllegalArgumentException("Filtros de reporte no reconocidos.");
        }
        return new Enlaces(pagina.hasPrevious() ? enlace(tipo, parametros, pagina.getNumber() - 1, false) : null,
            pagina.hasNext() ? enlace(tipo, parametros, pagina.getNumber() + 1, false) : null, enlace(tipo, parametros, 0, false), enlace(tipo, parametros, null, true));
    }

    private String enlace(TipoReporte tipo, Map<String, Object> parametros, Integer pagina, boolean impresion) {
        var valores = new LinkedHashMap<>(parametros);
        valores.put("pagina", pagina);
        String query = valores.entrySet().stream().filter(item -> item.getValue() != null && !item.getValue().toString().isEmpty())
            .map(item -> item.getKey() + "=" + URLEncoder.encode(item.getValue().toString(), StandardCharsets.UTF_8).replace("+", "%20"))
            .collect(Collectors.joining("&"));
        return tipo.getRuta() + (impresion ? "/impresion" : "") + (query.isEmpty() ? "" : "?" + query);
    }
}
