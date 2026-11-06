package sv.edu.ues.alimentacion_hospitalaria.insumos.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import sv.edu.ues.alimentacion_hospitalaria.insumos.service.AdministracionInsumosService;

@Controller
@RequestMapping("/insumos")
public class InsumosController {

    private final AdministracionInsumosService insumosService;

    public InsumosController(AdministracionInsumosService insumosService) {
        this.insumosService = insumosService;
    }


}
