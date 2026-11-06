package sv.edu.ues.alimentacion_hospitalaria.insumos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "insumos", uniqueConstraints = @UniqueConstraint(
    name = "uk_insumos_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotNull
    @Column(nullable = false, length = 20)
    private String unidad;

    // Porcentaje de merma por cocción o manipulación (0 a 100).
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal merma = BigDecimal.ZERO;

    // Forma en que se compra: "paquete", "saco", "caja"...
    @Size(max = 50)
    @Column(length = 50)
    private String presentacion;

    // Cuántas unidades base trae cada presentación (ej. 1000 g por paquete).
    @DecimalMin("0.00")
    @Column(name = "cantidad_presentacion", precision = 10, scale = 2)
    private BigDecimal cantidadPresentacion;

    // Costo por unidad base.
    @DecimalMin("0.00")
    @Column(name = "costo_unitario", precision = 10, scale = 4)
    private BigDecimal costoUnitario;

    public Insumo(String nombre, String unidad) {
        this.nombre = nombre;
        this.unidad = unidad;
    }
}
