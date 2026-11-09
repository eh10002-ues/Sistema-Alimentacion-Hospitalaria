package sv.edu.ues.alimentacion_hospitalaria.menus.persistence;

import java.util.List;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalleInsumo;

public interface MenuDetalleInsumoRepository extends JpaRepository<MenuDetalleInsumo, Long> {

    List<MenuDetalleInsumo> findByMenuDetalleId(Long menuDetalleId);

    boolean existsByInsumoId(Long insumoId);

    @Query("""
        select distinct detalleInsumo.insumo.id
        from MenuDetalleInsumo detalleInsumo
        where detalleInsumo.insumo.id in :insumoIds
        """)
    Set<Long> findInsumoIdsEnUso(@Param("insumoIds") Collection<Long> insumoIds);
}
