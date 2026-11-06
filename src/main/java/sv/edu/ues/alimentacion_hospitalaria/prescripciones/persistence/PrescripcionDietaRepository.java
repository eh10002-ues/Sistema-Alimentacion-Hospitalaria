package sv.edu.ues.alimentacion_hospitalaria.prescripciones.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;

public interface PrescripcionDietaRepository extends JpaRepository<PrescripcionDieta, Long> {

    // Historial del paciente, de la más reciente a la más antigua.
    List<PrescripcionDieta> findByPacienteIdOrderByFechaAutorizacionDescIdDesc(Long pacienteId);
}
