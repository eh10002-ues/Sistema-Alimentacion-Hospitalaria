package sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class AsignacionMenuForm {

    @NotNull(message = "Seleccione el paciente.")
    private Long pacienteId;

    @NotNull(message = "Seleccione el menú.")
    private Long menuId;

    @NotNull(message = "Ingrese la fecha de inicio.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;
}
