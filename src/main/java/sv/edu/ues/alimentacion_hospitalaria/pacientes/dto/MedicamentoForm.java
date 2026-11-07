package sv.edu.ues.alimentacion_hospitalaria.pacientes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicamentoForm {

    private Long id;

    @NotBlank(message = "El nombre del medicamento es obligatorio.")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres.")
    private String nombre;

    @NotBlank(message = "La dosis es obligatoria.")
    @Size(max = 50, message = "La dosis no puede superar 50 caracteres.")
    private String dosis;

    @NotNull(message = "La frecuencia es obligatoria.")
    @Min(value = 1, message = "La frecuencia debe ser de al menos 1 hora.")
    @Max(value = 24, message = "La frecuencia no puede superar 24 horas.")
    private Short frecuenciaHoras;

    @NotNull(message = "La hora de la primera dosis es obligatoria.")
    private LocalTime horaPrimeraDosis;
}
