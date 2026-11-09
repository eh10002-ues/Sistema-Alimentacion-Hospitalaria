package sv.edu.ues.alimentacion_hospitalaria.menus.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sv.edu.ues.alimentacion_hospitalaria.shared.domain.AuditableEntity;

@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoMenu tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_dieta", nullable = false, length = 30)
    private TipoDieta tipoDieta;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoMenu estado = EstadoMenu.BORRADOR;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("horaServicio ASC")
    @Setter(AccessLevel.NONE)
    private List<MenuDetalle> detalles = new ArrayList<>();

    public Menu(String nombre, TipoMenu tipo, TipoDieta tipoDieta) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.tipoDieta = tipoDieta;
    }

    public void agregarDetalle(MenuDetalle detalle) {
        detalles.add(detalle);
        detalle.setMenu(this);
    }

    public void quitarDetalle(MenuDetalle detalle) {
        detalles.remove(detalle);
        detalle.setMenu(null);
    }
}
