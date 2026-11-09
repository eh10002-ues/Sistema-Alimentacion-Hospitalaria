package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;

/** HU-02: entradas tipadas para las consultas del siguiente incremento. */
public final class FiltrosReporte {
    public static final int TAMANO_PAGINA = 20;

    private FiltrosReporte() { }

    public record Pacientes(
        @Min(value = 0, message = "La página no puede ser negativa.") Integer pagina,
        @Size(max = 100, message = "La búsqueda admite hasta 100 caracteres.") String buscar,
        @Positive(message = "Seleccione un piso válido.") Long pisoId,
        EstadoPaciente estado) {
        public Pacientes { pagina = pagina == null ? 0 : pagina; buscar = normalizar(buscar); }
    }

    public record Asignaciones(
        @Min(value = 0, message = "La página no puede ser negativa.") Integer pagina,
        @Size(max = 100, message = "La búsqueda admite hasta 100 caracteres.") String buscar,
        @Positive(message = "Seleccione un piso válido.") Long pisoId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        TipoDieta tipoDieta) {
        public Asignaciones { pagina = pagina == null ? 0 : pagina; buscar = normalizar(buscar); }
    }

    public record Menus(
        @Min(value = 0, message = "La página no puede ser negativa.") Integer pagina,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        TiempoComida tiempoComida,
        TipoDieta tipoDieta) {
        public Menus { pagina = pagina == null ? 0 : pagina; }
    }

    public record Ocupacion(
        @Min(value = 0, message = "La página no puede ser negativa.") Integer pagina,
        @Positive(message = "Seleccione un piso válido.") Long pisoId,
        @Size(max = 10, message = "El código de cama admite hasta 10 caracteres.") String cama,
        EstadoOcupacion ocupacion) {
        public Ocupacion { pagina = pagina == null ? 0 : pagina; cama = normalizar(cama); }
    }

    /** Estados de presentación, no valores nuevos en las tablas existentes. */
    public enum EstadoOcupacion {
        DISPONIBLE("Disponible"), OCUPADA("Ocupada"), INCONSISTENTE("Inconsistente");
        private final String etiqueta;
        EstadoOcupacion(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
