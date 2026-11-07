package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Turno;

@Getter
@Setter
public class AsignacionPisoItemForm {

    @NotNull(message = "Seleccione un encargado.")
    private Long usuarioId;

    @NotNull(message = "Seleccione el turno.")
    private Turno turno;
}
