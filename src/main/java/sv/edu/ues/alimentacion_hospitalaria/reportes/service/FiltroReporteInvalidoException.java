package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

/** Error de entrada conocido que puede mostrarse sin revelar detalles internos. */
public class FiltroReporteInvalidoException extends IllegalArgumentException {
    public FiltroReporteInvalidoException(String mensaje) {
        super(mensaje);
    }
}
