package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

/** Solo metadatos del catálogo; no expone entidades ni datos de pacientes. */
public record OpcionReporteVista(TipoReporte tipo, String titulo, String descripcion) {
    public static OpcionReporteVista desde(TipoReporte tipo) {
        return new OpcionReporteVista(tipo, tipo.getTitulo(), tipo.getDescripcion());
    }
}
