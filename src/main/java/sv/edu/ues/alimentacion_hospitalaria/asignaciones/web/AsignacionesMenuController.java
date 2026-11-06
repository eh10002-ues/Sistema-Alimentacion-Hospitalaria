package sv.edu.ues.alimentacion_hospitalaria.asignaciones.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.service.AdministracionAsignacionesMenuService;

@Controller
@RequestMapping("/asignaciones-menu")
public class AsignacionesMenuController {

    private final AdministracionAsignacionesMenuService asignacionesService;

    public AsignacionesMenuController(AdministracionAsignacionesMenuService asignacionesService) {
        this.asignacionesService = asignacionesService;
    }


}
