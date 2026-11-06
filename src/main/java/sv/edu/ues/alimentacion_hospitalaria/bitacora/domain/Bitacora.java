package sv.edu.ues.alimentacion_hospitalaria.bitacora.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

/** Registro de acciones relevantes: quién hizo qué sobre qué registro. */
@Entity
@Table(name = "bitacora")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String accion;

    @NotBlank
    @Size(max = 60)
    @Column(name = "tabla_afectada", nullable = false, length = 60)
    private String tablaAfectada;

    @Column(name = "id_afectado")
    private Long idAfectado;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_bitacora_usuario"))
    private Usuario usuario;

    public Bitacora(String accion, String tablaAfectada, Long idAfectado, Usuario usuario) {
        this.accion = accion;
        this.tablaAfectada = tablaAfectada;
        this.idAfectado = idAfectado;
        this.usuario = usuario;
    }

    @PrePersist
    private void asignarFecha() {
        if (fechaAccion == null) {
            fechaAccion = LocalDateTime.now();
        }
    }
}
