package sv.edu.ues.alimentacion_hospitalaria.pacientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;

@Getter
@Setter
public class AlergiaForm {

    private Long id;

    @NotBlank(message = "El nombre de la alergia es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    private String nombre;

    @NotNull(message = "La severidad es obligatoria.")
    private SeveridadAlergia severidad;
}
