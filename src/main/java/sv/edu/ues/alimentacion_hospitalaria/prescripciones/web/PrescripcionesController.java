package sv.edu.ues.alimentacion_hospitalaria.prescripciones.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.RegimenDietetico;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto.AutorizarPrescripcionForm;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.service.PrescripcionDietaService;

@Controller
@RequestMapping("/prescripciones")
public class PrescripcionesController {

    private final PrescripcionDietaService prescripciones;

    public PrescripcionesController(PrescripcionDietaService prescripciones) {
        this.prescripciones = prescripciones;
    }

    @InitBinder("formulario")
    void limitarCampos(WebDataBinder binder) {
        binder.setAllowedFields("regimen");
    }

    @GetMapping
    String listar(Model model) {
        model.addAttribute("pacientes", prescripciones.listarPacientesIngresados());
        return "prescripciones/listado";
    }

    @GetMapping("/pacientes/{id}")
    String evaluar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", new AutorizarPrescripcionForm());
        return prepararEvaluacion(id, model);
    }

    @PostMapping("/pacientes/{id}")
    String autorizar(@PathVariable Long id, @Valid @ModelAttribute("formulario") AutorizarPrescripcionForm formulario,
                     BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                var prescripcion = prescripciones.autorizar(id, formulario.getRegimen());
                redirect.addFlashAttribute("mensaje", "Dieta " + prescripcion.regimen().toLowerCase()
                    + " autorizada. La orden quedó en estado «" + prescripcion.estado() + "» y se envió a cocina.");
                return "redirect:/prescripciones/pacientes/" + id;
            } catch (IllegalArgumentException ex) {
                errores.reject("regla", ex.getMessage());
            }
        }
        return prepararEvaluacion(id, model);
    }

    private String prepararEvaluacion(Long id, Model model) {
        model.addAttribute("evaluacion", prescripciones.evaluar(id));
        model.addAttribute("regimenes", RegimenDietetico.values());
        return "prescripciones/evaluacion";
    }
}
