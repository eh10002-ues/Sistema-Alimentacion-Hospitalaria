package sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteMedicamento;

public interface PacienteMedicamentoRepository extends JpaRepository<PacienteMedicamento, Long> {

    List<PacienteMedicamento> findByPacienteId(Long pacienteId);
}
