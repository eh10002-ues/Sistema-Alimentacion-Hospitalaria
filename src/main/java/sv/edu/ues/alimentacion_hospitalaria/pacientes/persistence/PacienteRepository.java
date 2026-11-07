package sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Paciente> findByCodigoIgnoreCase(String codigo);

    List<Paciente> findByEstado(EstadoPaciente estado);

    List<Paciente> findByCamaId(Long camaId);

    List<Paciente> findByCamaIdAndEstado(Long camaId, EstadoPaciente estado);

    List<Paciente> findByMedicoResponsableId(Long usuarioId);

    @Query("""
        select p from Paciente p
        left join p.cama cama
        where (:buscar is null or :buscar = ''
            or lower(p.codigo) like lower(concat('%', :buscar, '%'))
            or lower(p.nombres) like lower(concat('%', :buscar, '%'))
            or lower(p.apellidos) like lower(concat('%', :buscar, '%')))
        and (:estado is null or p.estado = :estado)
        and (:pisoId is null or cama.piso.id = :pisoId)
        """)
    Page<Paciente> buscar(
        @Param("buscar") String buscar,
        @Param("estado") EstadoPaciente estado,
        @Param("pisoId") Long pisoId,
        Pageable pageable);
}
