package sv.edu.ues.alimentacion_hospitalaria.menus.domain;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;

/** Insumo y gramaje por ración de una comida. Base del cálculo de cantidades. */
@Entity
@Table(name = "menu_detalle_insumos", uniqueConstraints = @UniqueConstraint(
    name = "uk_menu_det_insumo", columnNames = {"menu_detalle_id", "insumo_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuDetalleInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_detalle_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_mdi_detalle"))
    private MenuDetalle menuDetalle;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_mdi_insumo"))
    private Insumo insumo;

    @NotNull
    @Positive
    @Column(name = "cantidad_por_racion", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadPorRacion;

    public MenuDetalleInsumo(Insumo insumo, BigDecimal cantidadPorRacion) {
        this.insumo = insumo;
        this.cantidadPorRacion = cantidadPorRacion;
    }
}
