package sv.edu.ues.alimentacion_hospitalaria.menus.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;

public interface MenuDetalleRepository extends JpaRepository<MenuDetalle, Long> {

    List<MenuDetalle> findByMenuIdOrderByHoraServicioAsc(Long menuId);
}
