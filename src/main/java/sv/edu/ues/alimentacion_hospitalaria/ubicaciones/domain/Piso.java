package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pisos", uniqueConstraints = @UniqueConstraint(
    name = "uk_pisos_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Piso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String codigo;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    public Piso(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }
}
