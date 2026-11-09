package sv.edu.ues.alimentacion_hospitalaria.asignaciones.web;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto.AsignacionMenuForm;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.service.AdministracionAsignacionesMenuService;

/** Rutas bajo /asignaciones, según ReglasAcceso (consulta y alta: Administración, Médico y Nutrición). */
@Controller
@RequestMapping("/asignaciones")
public class AsignacionesMenuController {

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");

    private final AdministracionAsignacionesMenuService asignacionesService;

    public AsignacionesMenuController(AdministracionAsignacionesMenuService asignacionesService) {
        this.asignacionesService = asignacionesService;
    }

    @InitBinder("formulario")
    void limitarCampos(WebDataBinder binder) {
        binder.setAllowedFields("pacienteId", "menuId", "fechaInicio", "fechaFin");
    }

    @GetMapping
    String listar(@RequestParam(defaultValue = "false") boolean pendientes, Model model) {
        model.addAttribute("asignaciones", asignacionesService.listar(pendientes));
        model.addAttribute("pendientes", pendientes);
        return "asignaciones/listado";
    }

    @GetMapping("/nueva")
    String nueva(Model model) {
        AsignacionMenuForm formulario = new AsignacionMenuForm();
        formulario.setFechaInicio(hoy());
        model.addAttribute("formulario", formulario);
        return prepararFormulario(model);
    }

    @PostMapping
    String crear(@Valid @ModelAttribute("formulario") AsignacionMenuForm formulario, BindingResult errores,
                 Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                asignacionesService.crear(formulario, hoy());
                redirect.addFlashAttribute("mensaje",
                    "Asignación registrada. Queda pendiente hasta que Nutrición la apruebe.");
                return "redirect:/asignaciones?pendientes=true";
            } catch (IllegalArgumentException ex) {
                errores.reject("regla", ex.getMessage());
            }
        }
        return prepararFormulario(model);
    }

    @PostMapping("/{id}/aprobar")
    String aprobar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            asignacionesService.aprobar(id, hoy());
            redirect.addFlashAttribute("mensaje", "Asignación aprobada.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/asignaciones?pendientes=true";
    }

    private String prepararFormulario(Model model) {
        model.addAttribute("pacientes", asignacionesService.pacientesIngresados());
        model.addAttribute("menus", asignacionesService.menusActivos());
        return "asignaciones/formulario";
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZONA_EL_SALVADOR);
    }
}
