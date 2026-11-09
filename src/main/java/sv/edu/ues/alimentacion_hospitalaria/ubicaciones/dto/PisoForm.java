package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PisoForm {

    @NotBlank(message = "Ingrese el código del piso.")
    @Size(max = 10, message = "El código admite hasta 10 caracteres.")
    private String codigo;

    @NotBlank(message = "Ingrese el nombre del piso.")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres.")
    private String nombre;
}
