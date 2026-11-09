package sv.edu.ues.alimentacion_hospitalaria.reportes.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.CatalogoReportesService;

/** Etiquetas de presentación para la cabecera imprimible; no contiene entidades. */
@Component
public class ResumenFiltrosReportes {
    public record FiltroAplicado(String nombre, String valor) { }
    private final CatalogoReportesService catalogo;

    public ResumenFiltrosReportes(CatalogoReportesService catalogo) { this.catalogo = catalogo; }

    public List<FiltroAplicado> crear(Object filtros, LocalDate fecha) {
        var resultado = new ArrayList<FiltroAplicado>();
        switch (filtros) {
            case FiltrosReporte.Pacientes f -> {
                resultado.add(new FiltroAplicado("Paciente", f.buscar().isEmpty() ? "Todos" : f.buscar()));
                resultado.add(new FiltroAplicado("Piso", piso(f.pisoId())));
                resultado.add(new FiltroAplicado("Estado", f.estado() == null ? "Todos" : f.estado().name().equals("INGRESADO") ? "Ingresado" : "Alta"));
            }
            case FiltrosReporte.Asignaciones f -> {
                resultado.add(new FiltroAplicado("Paciente", f.buscar().isEmpty() ? "Todos" : f.buscar()));
                resultado.add(new FiltroAplicado("Piso", piso(f.pisoId())));
                resultado.add(new FiltroAplicado("Tipo de dieta", f.tipoDieta() == null ? "Todas" : f.tipoDieta().getEtiqueta()));
            }
            case FiltrosReporte.Menus f -> {
                resultado.add(new FiltroAplicado("Tipo de dieta", f.tipoDieta() == null ? "Todas" : f.tipoDieta().getEtiqueta()));
                resultado.add(new FiltroAplicado("Tiempo de comida", f.tiempoComida() == null ? "Todos" : f.tiempoComida().getEtiqueta()));
            }
            case FiltrosReporte.Ocupacion f -> {
                resultado.add(new FiltroAplicado("Piso", piso(f.pisoId())));
                resultado.add(new FiltroAplicado("Cama", f.cama().isEmpty() ? "Todas" : f.cama()));
                resultado.add(new FiltroAplicado("Ocupación", f.ocupacion() == null ? "Todas" : f.ocupacion().getEtiqueta()));
            }
            default -> throw new IllegalArgumentException("Filtros de reporte no reconocidos.");
        }
        if (fecha != null) { resultado.add(new FiltroAplicado("Fecha de consulta", fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))); }
        return List.copyOf(resultado);
    }

    private String piso(Long id) {
        if (id == null) { return "Todos"; }
        return catalogo.pisos().stream().filter(piso -> piso.id().equals(id))
            .map(piso -> piso.codigo() + " - " + piso.nombre()).findFirst().orElse("Piso no disponible");
    }
}
