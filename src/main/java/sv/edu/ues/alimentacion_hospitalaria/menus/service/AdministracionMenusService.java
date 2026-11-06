package sv.edu.ues.alimentacion_hospitalaria.menus.service;

import org.springframework.stereotype.Service;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuDetalleInsumoRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuDetalleRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;

@Service
public class AdministracionMenusService {

    private final MenuRepository menuRepository;
    private final MenuDetalleRepository menuDetalleRepository;
    private final MenuDetalleInsumoRepository menuDetalleInsumoRepository;

    public AdministracionMenusService(MenuRepository menuRepository,
                                      MenuDetalleRepository menuDetalleRepository,
                                      MenuDetalleInsumoRepository menuDetalleInsumoRepository) {
        this.menuRepository = menuRepository;
        this.menuDetalleRepository = menuDetalleRepository;
        this.menuDetalleInsumoRepository = menuDetalleInsumoRepository;
    }

    // TODO: creación de menús generales/personalizados, comidas e insumos por ración.
}
