package sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Paciente> findByEstado(EstadoPaciente estado);

    Optional<Paciente> findByCamaId(Long camaId);

    List<Paciente> findByMedicoResponsableId(Long usuarioId);
}
