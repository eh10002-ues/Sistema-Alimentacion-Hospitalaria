package sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;

public interface PacienteAlergiaRepository extends JpaRepository<PacienteAlergia, Long> {

    List<PacienteAlergia> findByPacienteId(Long pacienteId);

    List<PacienteAlergia> findBySeveridad(SeveridadAlergia severidad);
}
