package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;

public interface PisoRepository extends JpaRepository<Piso, Long> {

    Optional<Piso> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
