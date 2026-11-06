package sv.edu.ues.alimentacion_hospitalaria.pacientes.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.service.AdministracionPacientesService;

@Controller
@RequestMapping("/pacientes")
public class PacientesController {

    private final AdministracionPacientesService pacientesService;

    public PacientesController(AdministracionPacientesService pacientesService) {
        this.pacientesService = pacientesService;
    }

    // TODO: endpoints y vistas de pacientes.
}
