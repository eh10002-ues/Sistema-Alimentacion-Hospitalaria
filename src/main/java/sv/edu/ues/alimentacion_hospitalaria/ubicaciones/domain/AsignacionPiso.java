package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

/** Encargado de un piso para un turno. Un solo encargado por piso y turno. */
@Entity
@Table(name = "asignaciones_piso", uniqueConstraints = @UniqueConstraint(
    name = "uk_asig_piso_turno", columnNames = {"piso_id", "turno"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsignacionPiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "piso_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_asig_piso_piso"))
    private Piso piso;

    // Debe ser un usuario con rol ENCARGADO_PISO (validar en el service).
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_asig_piso_usuario"))
    private Usuario usuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Turno turno;

    public AsignacionPiso(Piso piso, Usuario usuario, Turno turno) {
        this.piso = piso;
        this.usuario = usuario;
        this.turno = turno;
    }
}
