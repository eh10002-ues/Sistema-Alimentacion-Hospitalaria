package sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteEnfermedad;

public interface PacienteEnfermedadRepository extends JpaRepository<PacienteEnfermedad, Long> {

    List<PacienteEnfermedad> findByPacienteId(Long pacienteId);
}
