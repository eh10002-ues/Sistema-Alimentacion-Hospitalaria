package sv.edu.ues.alimentacion_hospitalaria.pacientes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Medicamento del paciente con su horario de administración. */
@Entity
@Table(name = "paciente_medicamentos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PacienteMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_pac_med_paciente"))
    private Paciente paciente;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String dosis;

    // Cada cuántas horas se administra (SMALLINT en la base de datos).
    @Positive
    @Column(name = "frecuencia_horas", nullable = false)
    private short frecuenciaHoras;

    @NotNull
    @Column(name = "hora_primera_dosis", nullable = false)
    private LocalTime horaPrimeraDosis;

    public PacienteMedicamento(String nombre, String dosis, short frecuenciaHoras,
                               LocalTime horaPrimeraDosis) {
        this.nombre = nombre;
        this.dosis = dosis;
        this.frecuenciaHoras = frecuenciaHoras;
        this.horaPrimeraDosis = horaPrimeraDosis;
    }
}
