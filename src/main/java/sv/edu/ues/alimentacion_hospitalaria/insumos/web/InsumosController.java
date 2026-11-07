package sv.edu.ues.alimentacion_hospitalaria.insumos.web;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.PeriodoProyeccion;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.ProyeccionInsumosForm;
import sv.edu.ues.alimentacion_hospitalaria.insumos.service.ProyeccionInsumosService;

@Controller
@RequestMapping("/insumos")
public class InsumosController {

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");

    private final ProyeccionInsumosService proyeccionService;

    public InsumosController(ProyeccionInsumosService proyeccionService) {
        this.proyeccionService = proyeccionService;
    }

    @InitBinder("formulario")
    void limitarCampos(WebDataBinder binder) {
        binder.setAllowedFields("periodo", "margenFraccion");
    }

    @GetMapping("/proyeccion")
    String proyeccion(@Valid @ModelAttribute("formulario") ProyeccionInsumosForm formulario,
                      BindingResult errores,
                      Model model) {
        model.addAttribute("periodos", PeriodoProyeccion.values());
        if (!errores.hasErrors()) {
            try {
                model.addAttribute("proyeccion", proyeccionService.proyectar(
                    formulario.getPeriodo(), formulario.getMargenFraccion(),
                    LocalDate.now(ZONA_EL_SALVADOR)));
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorProyeccion", ex.getMessage());
            }
        }
        return "insumos/proyeccion";
    }
}
