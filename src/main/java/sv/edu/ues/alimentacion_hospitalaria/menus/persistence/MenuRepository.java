package sv.edu.ues.alimentacion_hospitalaria.menus.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByTipo(TipoMenu tipo);

    List<Menu> findByTipoDieta(TipoDieta tipoDieta);

    List<Menu> findByEstado(EstadoMenu estado);
}
