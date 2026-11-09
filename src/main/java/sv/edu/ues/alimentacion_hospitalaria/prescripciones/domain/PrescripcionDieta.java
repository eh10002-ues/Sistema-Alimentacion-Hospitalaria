package sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain;

import java.time.Instant;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.shared.domain.AuditableEntity;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

/**
 * Régimen dietético prescrito a un paciente (SAH-2). Es inmutable: una nueva
 * autorización crea otro registro y el historial anterior se conserva.
 */
@Entity
@Table(name = "prescripciones_dieta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrescripcionDieta extends AuditableEntity {

    private static final Set<Rol> ROLES_AUTORIZADORES = Set.of(Rol.ROLE_MEDICO, Rol.ROLE_NUTRICIONISTA);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_presc_dieta_paciente"))
    private Paciente paciente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_dieta", nullable = false, length = 30)
    private TipoDieta tipoDieta;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPrescripcion estado;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autorizado_por_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_presc_dieta_autorizador"))
    private Usuario autorizadoPor;

    @NotNull
    @Column(name = "fecha_autorizacion", nullable = false)
    private Instant fechaAutorizacion;

    private PrescripcionDieta(Paciente paciente, TipoDieta tipoDieta, Usuario autorizadoPor, Instant fechaAutorizacion) {
        this.paciente = paciente;
        this.tipoDieta = tipoDieta;
        this.autorizadoPor = autorizadoPor;
        this.fechaAutorizacion = fechaAutorizacion;
        this.estado = EstadoPrescripcion.APROBADO;
    }

    /** Crea la prescripción en estado APROBADO tras validar al autorizador y al paciente. */
    public static PrescripcionDieta autorizar(Paciente paciente, RegimenDietetico regimen, Usuario autorizador, Instant ahora) {
        if (paciente == null || regimen == null || autorizador == null || ahora == null) {
            throw new IllegalArgumentException("Seleccione el paciente y el régimen dietético.");
        }
        if (!autorizador.isActivo() || !ROLES_AUTORIZADORES.contains(autorizador.getRol())) {
            throw new IllegalArgumentException("Solo un médico o nutricionista activo puede autorizar la dieta.");
        }
        if (paciente.getEstado() != EstadoPaciente.INGRESADO) {
            throw new IllegalArgumentException("Solo se puede prescribir dieta a pacientes ingresados.");
        }
        return new PrescripcionDieta(paciente, regimen.getTipoDieta(), autorizador, ahora);
    }

    public RegimenDietetico getRegimen() {
        return RegimenDietetico.desde(tipoDieta);
    }
}
