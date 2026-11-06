package sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;

public interface AsignacionMenuRepository extends JpaRepository<AsignacionMenu, Long> {

    // Menús de un paciente
    List<AsignacionMenu> findByPacienteIdOrderByFechaInicioDesc(Long pacienteId);

    List<AsignacionMenu> findByPacienteIdAndActivaTrue(Long pacienteId);

    // Pacientes de un menú
    List<AsignacionMenu> findByMenuIdAndActivaTrue(Long menuId);

    // Asignaciones sin aprobar
    List<AsignacionMenu> findByAprobadoPorIsNull();
}
