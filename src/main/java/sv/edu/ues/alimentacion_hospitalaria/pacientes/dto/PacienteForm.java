package sv.edu.ues.alimentacion_hospitalaria.pacientes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PacienteForm {

    @NotBlank
    @Size(max = 20)
    private String codigo;

    @NotBlank
    @Size(max = 100)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    private String apellidos;

    @NotNull
    @PastOrPresent
    private LocalDate fechaNacimiento;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal pesoKg;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal alturaCm;

    @NotNull
    private Long medicoResponsableId;

    @NotNull
    private Long pisoId;

    @NotNull
    private Long camaId;
}
