package sv.edu.ues.alimentacion_hospitalaria.cocina.dto;

import java.time.LocalDate;
import java.util.List;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;

/**
 * SC-03.1: pacientes internos asociados a un régimen dietético (TipoDieta) en la fecha consultada,
 * con el menú concreto que reciben. Solo expone datos operativos; no incluye información clínica.
 */
public record PacientesPorMenuVista(TipoDieta tipoDieta, LocalDate fecha, List<Fila> pacientes) {

    public record Fila(String codigo, String nombreCompleto, String piso, String cama, String menu,
                       LocalDate fechaInicio, LocalDate fechaFin) {
    }
}
