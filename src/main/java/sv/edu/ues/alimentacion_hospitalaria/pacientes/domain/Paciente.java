package sv.edu.ues.alimentacion_hospitalaria.pacientes.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sv.edu.ues.alimentacion_hospitalaria.shared.domain.AuditableEntity;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

@Entity
@Table(name = "pacientes", uniqueConstraints = @UniqueConstraint(
    name = "uk_pacientes_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paciente extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String codigo;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String apellidos;

    @NotNull
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @NotNull
    @Positive
    @Column(name = "peso_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @NotNull
    @Positive
    @Column(name = "altura_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal alturaCm;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPaciente estado = EstadoPaciente.INGRESADO;

    // Null cuando el paciente no tiene cama (alta). UNIQUE: una cama, un paciente.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cama_id", unique = true,
        foreignKey = @ForeignKey(name = "fk_pacientes_cama"))
    private Cama cama;

    // Usuario con rol MEDICO o NUTRICIONISTA (validar en el service).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_responsable_id",
        foreignKey = @ForeignKey(name = "fk_pacientes_medico"))
    private Usuario medicoResponsable;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<PacienteEnfermedad> enfermedades = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<PacienteAlergia> alergias = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<PacienteMedicamento> medicamentos = new ArrayList<>();

    public Paciente(String codigo, String nombres, String apellidos,
                    LocalDate fechaNacimiento, BigDecimal pesoKg) {
        this.codigo = codigo;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.pesoKg = pesoKg;
    }

    /** La edad no se guarda: se calcula desde la fecha de nacimiento. */
    public int calcularEdad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public void agregarEnfermedad(PacienteEnfermedad enfermedad) {
        enfermedades.add(enfermedad);
        enfermedad.setPaciente(this);
    }

    public void quitarEnfermedad(PacienteEnfermedad enfermedad) {
        enfermedades.remove(enfermedad);
        enfermedad.setPaciente(null);
    }

    public void agregarAlergia(PacienteAlergia alergia) {
        alergias.add(alergia);
        alergia.setPaciente(this);
    }

    public void quitarAlergia(PacienteAlergia alergia) {
        alergias.remove(alergia);
        alergia.setPaciente(null);
    }

    public void agregarMedicamento(PacienteMedicamento medicamento) {
        medicamentos.add(medicamento);
        medicamento.setPaciente(this);
    }

    public void quitarMedicamento(PacienteMedicamento medicamento) {
        medicamentos.remove(medicamento);
        medicamento.setPaciente(null);
    }
}
