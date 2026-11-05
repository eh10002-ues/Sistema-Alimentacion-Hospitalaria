package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;

public interface CamaRepository extends JpaRepository<Cama, Long> {

    List<Cama> findByPisoId(Long pisoId);

    boolean existsByPisoIdAndNumero(Long pisoId, String numero);
}
