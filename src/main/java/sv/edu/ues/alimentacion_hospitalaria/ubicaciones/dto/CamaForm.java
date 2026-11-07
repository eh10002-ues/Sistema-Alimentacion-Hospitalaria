package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CamaForm {

    @Valid
    private List<CodigoCama> camas = new ArrayList<>(List.of(new CodigoCama()));

    public boolean estaVacio() {
        return camas == null || camas.stream()
            .allMatch(cama -> cama.getNumero() == null || cama.getNumero().isBlank());
    }

    @Getter
    @Setter
    public static class CodigoCama {
        @jakarta.validation.constraints.NotBlank(message = "Ingrese el código o número de la cama.")
        @jakarta.validation.constraints.Size(max = 10, message = "El código de cama admite hasta 10 caracteres.")
        private String numero;
    }
}
