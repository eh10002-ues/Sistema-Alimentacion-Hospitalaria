package sv.edu.ues.alimentacion_hospitalaria.menus.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import sv.edu.ues.alimentacion_hospitalaria.menus.service.AdministracionMenusService;

@Controller
@RequestMapping("/menus")
public class MenusController {

    private final AdministracionMenusService menusService;

    public MenusController(AdministracionMenusService menusService) {
        this.menusService = menusService;
    }

    // TODO: endpoints y vistas de menús.
}
