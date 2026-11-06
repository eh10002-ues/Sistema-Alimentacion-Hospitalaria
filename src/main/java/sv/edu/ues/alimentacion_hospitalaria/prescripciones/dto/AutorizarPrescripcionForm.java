package sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.RegimenDietetico;

@Getter
@Setter
public class AutorizarPrescripcionForm {

    @NotNull(message = "Seleccione un régimen dietético.")
    private RegimenDietetico regimen;
}
