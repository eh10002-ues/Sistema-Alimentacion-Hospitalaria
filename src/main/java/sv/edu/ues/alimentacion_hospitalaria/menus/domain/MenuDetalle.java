package sv.edu.ues.alimentacion_hospitalaria.menus.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una comida del menú (desayuno, almuerzo...) con su hora de servicio. */
@Entity
@Table(name = "menu_detalles", uniqueConstraints = @UniqueConstraint(
    name = "uk_menu_detalle", columnNames = {"menu_id", "tiempo_comida"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_menu_det_menu"))
    private Menu menu;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tiempo_comida", nullable = false, length = 30)
    private TiempoComida tiempoComida;

    @NotNull
    @Column(name = "hora_servicio", nullable = false)
    private LocalTime horaServicio;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String platillo;

    @OneToMany(mappedBy = "menuDetalle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<MenuDetalleInsumo> insumos = new ArrayList<>();

    public MenuDetalle(TiempoComida tiempoComida, LocalTime horaServicio, String platillo) {
        this.tiempoComida = tiempoComida;
        this.horaServicio = horaServicio;
        this.platillo = platillo;
    }

    public void agregarInsumo(MenuDetalleInsumo insumo) {
        insumos.add(insumo);
        insumo.setMenuDetalle(this);
    }

    public void quitarInsumo(MenuDetalleInsumo insumo) {
        insumos.remove(insumo);
        insumo.setMenuDetalle(null);
    }
}
