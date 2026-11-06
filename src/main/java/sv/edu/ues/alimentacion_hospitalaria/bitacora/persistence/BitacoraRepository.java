package sv.edu.ues.alimentacion_hospitalaria.bitacora.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.domain.Bitacora;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    List<Bitacora> findByUsuarioIdOrderByFechaAccionDesc(Long usuarioId);

    List<Bitacora> findByTablaAfectadaAndIdAfectadoOrderByFechaAccionDesc(
        String tablaAfectada, Long idAfectado);
}
