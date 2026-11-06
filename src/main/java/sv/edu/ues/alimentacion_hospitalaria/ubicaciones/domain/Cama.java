package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
//se asigna solo un numero de cama por cada piso, ej.: no pueden haber 2 camas #10 en piso 5
@Table(name = "camas", uniqueConstraints = @UniqueConstraint(
    name = "uk_camas_piso_numero", columnNames = {"piso_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cama {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "piso_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_camas_piso"))
    private Piso piso;

    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String numero;

    public Cama(Piso piso, String numero) {
        this.piso = piso;
        this.numero = numero;
    }
}
