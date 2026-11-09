package sv.edu.ues.alimentacion_hospitalaria.insumos.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    Optional<Insumo> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Insumo> findByNombreContainingIgnoreCase(String texto);

    Page<Insumo> findByNombreContainingIgnoreCase(String texto, Pageable pageable);
}
