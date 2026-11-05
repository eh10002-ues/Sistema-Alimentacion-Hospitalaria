package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.AsignacionPiso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Turno;

public interface AsignacionPisoRepository extends JpaRepository<AsignacionPiso, Long> {

    List<AsignacionPiso> findByPisoId(Long pisoId);

    Optional<AsignacionPiso> findByPisoIdAndTurno(Long pisoId, Turno turno);

    //para traer por encargado
    List<AsignacionPiso> findByUsuarioId(Long usuarioId);
}
