package sv.edu.ues.alimentacion_hospitalaria.pacientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnfermedadForm {

    private Long id;

    @NotBlank(message = "El nombre de la enfermedad es obligatorio.")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres.")
    private String nombre;
}
