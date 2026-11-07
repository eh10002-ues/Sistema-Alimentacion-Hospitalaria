package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsignacionPisoForm {

    @Valid
    private List<AsignacionPisoItemForm> asignaciones = new ArrayList<>(List.of(new AsignacionPisoItemForm()));

    public boolean estaVacio() {
        return asignaciones == null || asignaciones.stream()
            .allMatch(asignacion -> asignacion.getUsuarioId() == null && asignacion.getTurno() == null);
    }
}
