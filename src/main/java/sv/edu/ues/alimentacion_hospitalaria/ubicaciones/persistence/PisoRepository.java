package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;

public interface PisoRepository extends JpaRepository<Piso, Long> {

    Optional<Piso> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("""
        select piso from Piso piso
        where (:busqueda = '' or lower(piso.codigo) like lower(concat('%', :busqueda, '%'))
            or lower(piso.nombre) like lower(concat('%', :busqueda, '%')))
        """)
    Page<Piso> buscar(@Param("busqueda") String busqueda, Pageable pageable);
}
