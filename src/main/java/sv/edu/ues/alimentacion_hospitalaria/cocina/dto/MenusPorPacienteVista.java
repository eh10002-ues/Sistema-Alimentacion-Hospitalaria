package sv.edu.ues.alimentacion_hospitalaria.cocina.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * SC-03.2: historial de menús aprobados de un paciente interno, con la dieta vigente
 * en la fecha consultada y sus tiempos de comida y horarios de servicio.
 */
public record MenusPorPacienteVista(Long pacienteId, String codigo, String nombreCompleto, String piso,
                                    String cama, LocalDate fecha, List<MenuVigente> vigentes,
                                    List<Historial> historial) {

    public record MenuVigente(String menu, String tipoDieta, LocalDate fechaInicio, LocalDate fechaFin,
                              List<Tiempo> tiempos) {
    }

    public record Tiempo(String tiempoComida, LocalTime horaServicio, String platillo) {
    }

    public record Historial(String menu, String tipoDieta, LocalDate fechaInicio, LocalDate fechaFin,
                            EstadoAsignacion estado) {
    }

    public enum EstadoAsignacion {
        VIGENTE("Vigente"),
        PROGRAMADA("Programada"),
        FINALIZADA("Finalizada"),
        MENU_NO_DISPONIBLE("Menú no disponible");

        private final String etiqueta;

        EstadoAsignacion(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }
}
