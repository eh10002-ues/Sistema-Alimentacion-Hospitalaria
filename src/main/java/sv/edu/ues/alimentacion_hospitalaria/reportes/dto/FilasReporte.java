package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte.EstadoOcupacion;

/** HU-02: salidas de lectura sin entidades JPA ni información clínica innecesaria. */
public final class FilasReporte {
    private FilasReporte() { }

    public record OpcionPiso(Long id, String codigo, String nombre) { }
    public record Paciente(String codigo, String nombre, String piso, String cama,
                           EstadoPaciente estado, boolean ubicacionHistorica) { }
    public record PacienteUbicacion(String codigo, String nombre, String piso, String cama) { }
    public record Tiempo(TiempoComida tiempoComida, LocalTime horaServicio) { }

    public sealed interface Asignacion permits AsignacionClinica, AsignacionPiso, AsignacionCocina { }
    public record AsignacionClinica(PacienteUbicacion paciente, TipoDieta dieta, String menu,
                                    LocalDate inicio, LocalDate fin, String estado, String responsable,
                                    List<Tiempo> tiempos) implements Asignacion {
        public AsignacionClinica { tiempos = List.copyOf(tiempos); }
    }
    public record AsignacionPiso(PacienteUbicacion paciente, TipoDieta dieta, String menu,
                                 String estado) implements Asignacion { }
    public record AsignacionCocina(PacienteUbicacion paciente, TipoDieta dieta, String menu,
                                   LocalDate fecha, List<Tiempo> tiempos) implements Asignacion {
        public AsignacionCocina { tiempos = List.copyOf(tiempos); }
    }
    public record Menu(LocalDate fecha, TiempoComida tiempoComida, LocalTime horaServicio,
                       String nombre, TipoDieta dieta, EstadoMenu estado) { }
    public record Ocupante(String codigo, String nombre) { }
    public record Ocupacion(String piso, String cama, EstadoOcupacion estado, List<Ocupante> ocupantes) {
        public Ocupacion { ocupantes = List.copyOf(ocupantes); }
    }
    public record ResultadoFechado<T>(LocalDate fechaConsulta, Page<T> pagina) { }
}
