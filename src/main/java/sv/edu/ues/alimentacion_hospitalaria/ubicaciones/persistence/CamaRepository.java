package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;

public interface CamaRepository extends JpaRepository<Cama, Long> {

    List<Cama> findByPisoId(Long pisoId);

    boolean existsByPisoIdAndNumero(Long pisoId, String numero);

    boolean existsByPisoIdAndNumeroIgnoreCase(Long pisoId, String numero);

    long countByPisoId(Long pisoId);

    List<Cama> findByPisoIdOrderByNumeroAsc(Long pisoId);

    boolean existsByPisoIdAndNumeroIgnoreCaseAndIdNot(Long pisoId, String numero, Long id);

    @Query("""
        select cama from Cama cama
        where cama.piso.id = :pisoId
        and not exists (
            select paciente.id from Paciente paciente
            where paciente.cama = cama and paciente.estado = :estadoIngresado
        )
        order by cama.numero asc
        """)
    List<Cama> buscarDisponibles(
        @Param("pisoId") Long pisoId,
        @Param("estadoIngresado") EstadoPaciente estadoIngresado);
}
