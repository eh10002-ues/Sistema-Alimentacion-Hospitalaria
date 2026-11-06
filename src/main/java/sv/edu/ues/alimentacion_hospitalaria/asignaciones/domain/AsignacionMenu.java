package sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain;

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
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.shared.domain.AuditableEntity;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

/**
 * Menú asignado a un paciente. De aquí salen las consultas "pacientes por menú"
 * y "menús por paciente". Regla de servicio: solo se asigna con aprobación del
 * médico o nutricionista (aprobadoPor + fechaAprobacion).
 */
@Entity
@Table(name = "asignaciones_menu")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsignacionMenu extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_asig_menu_paciente"))
    private Paciente paciente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_asig_menu_menu"))
    private Menu menu;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignado_por_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_asig_menu_asignador"))
    private Usuario asignadoPor;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activa = true;

    // Usuario con rol MEDICO o NUTRICIONISTA que aprueba. Null mientras esté pendiente.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por_id",
        foreignKey = @ForeignKey(name = "fk_asig_menu_aprobador"))
    private Usuario aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    public AsignacionMenu(Paciente paciente, Menu menu, Usuario asignadoPor, LocalDate fechaInicio) {
        this.paciente = paciente;
        this.menu = menu;
        this.asignadoPor = asignadoPor;
        this.fechaInicio = fechaInicio;
    }
}
