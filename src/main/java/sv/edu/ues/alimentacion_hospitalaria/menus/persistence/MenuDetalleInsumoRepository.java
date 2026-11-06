package sv.edu.ues.alimentacion_hospitalaria.menus.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalleInsumo;

public interface MenuDetalleInsumoRepository extends JpaRepository<MenuDetalleInsumo, Long> {

    List<MenuDetalleInsumo> findByMenuDetalleId(Long menuDetalleId);
}
