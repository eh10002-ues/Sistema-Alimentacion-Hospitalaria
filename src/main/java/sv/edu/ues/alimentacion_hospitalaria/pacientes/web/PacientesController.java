package sv.edu.ues.alimentacion_hospitalaria.pacientes.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.AlergiaForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.EnfermedadForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.service.AdministracionPacientesService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/pacientes")
public class PacientesController {

    private final AdministracionPacientesService pacientesService;

    public PacientesController(AdministracionPacientesService pacientesService) {
        this.pacientesService = pacientesService;
    }

    @GetMapping
    String listar(@RequestParam(defaultValue = "0") int pagina,
                  @RequestParam(defaultValue = "") String buscar,
                  @RequestParam(required = false) EstadoPaciente estado,
                  @RequestParam(required = false) Long pisoId,
                  Model model) {
        model.addAttribute("pagina", pacientesService.listar(pagina, buscar, estado, pisoId));
        model.addAttribute("buscar", buscar);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("pisoSeleccionadoId", pisoId);
        model.addAttribute("estados", EstadoPaciente.values());
        model.addAttribute("pisos", pacientesService.pisos());
        return "pacientes/listado";
    }

    @GetMapping("/{id}")
    String ver(@PathVariable Long id, Model model) {
        model.addAttribute("paciente", pacientesService.consultar(id));
        return "pacientes/detalle";
    }

    @GetMapping("/nuevo")
    String nuevo(Model model) {
        model.addAttribute("formulario", new PacienteForm());
        return prepararFormulario(model);
    }

    @PostMapping
    String crear(@Valid @ModelAttribute("formulario") PacienteForm formulario,
                 BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                pacientesService.crear(formulario);
                return redireccionExito(redirect, "Paciente ingresado correctamente.");
            } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                errores.reject("regla", ex instanceof DataIntegrityViolationException
                    ? "La cama seleccionada ya está ocupada por otro paciente ingresado."
                    : ex.getMessage());
            }
        }
        return prepararFormulario(model);
    }

    @GetMapping("/{id}/editar")
    String editar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", pacientesService.consultarParaEditar(id));
        model.addAttribute("pacienteId", id);
        model.addAttribute("edicion", true);
        return prepararFormularioEdicion(model, id);
    }

    @PostMapping("/{id}/editar")
    String guardarEdicion(@PathVariable Long id,
                          @Valid @ModelAttribute("formulario") PacienteForm formulario,
                          BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                pacientesService.actualizar(id, formulario);
                return redireccionExito(redirect, "Paciente actualizado correctamente.");
            } catch (IllegalArgumentException ex) {
                errores.reject("regla", ex.getMessage());
            }
        }
        model.addAttribute("pacienteId", id);
        model.addAttribute("edicion", true);
        return prepararFormularioEdicion(model, id);
    }

    @GetMapping("/{id}/alergias")
    String gestionarAlergias(@PathVariable Long id, Model model) {
        AlergiaForm formulario = new AlergiaForm();
        model.addAttribute("formulario", formulario);
        return prepararAlergias(model, id);
    }

    @GetMapping("/{id}/enfermedades")
    String gestionarEnfermedades(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", new EnfermedadForm());
        return prepararEnfermedades(model, id);
    }

    @PostMapping("/{id}/enfermedades")
    String agregarEnfermedad(@PathVariable Long id, @Valid @ModelAttribute("formulario") EnfermedadForm formulario,
                             BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            pacientesService.agregarEnfermedad(id, formulario);
            redirect.addFlashAttribute("mensaje", "Enfermedad agregada correctamente.");
            return "redirect:/pacientes/" + id + "/enfermedades";
        }
        return prepararEnfermedades(model, id);
    }

    @PostMapping("/{id}/enfermedades/{enfermedadId}/editar")
    String editarEnfermedad(@PathVariable Long id, @PathVariable Long enfermedadId,
                            @Valid @ModelAttribute("formulario") EnfermedadForm formulario,
                            BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            pacientesService.actualizarEnfermedad(id, enfermedadId, formulario);
            redirect.addFlashAttribute("mensaje", "Enfermedad actualizada correctamente.");
            return "redirect:/pacientes/" + id + "/enfermedades";
        }
        return prepararEnfermedades(model, id);
    }

    @PostMapping("/{id}/enfermedades/{enfermedadId}/eliminar")
    String eliminarEnfermedad(@PathVariable Long id, @PathVariable Long enfermedadId, RedirectAttributes redirect) {
        pacientesService.eliminarEnfermedad(id, enfermedadId);
        redirect.addFlashAttribute("mensaje", "Enfermedad eliminada correctamente.");
        return "redirect:/pacientes/" + id + "/enfermedades";
    }

    @PostMapping("/{id}/alergias")
    String agregarAlergia(@PathVariable Long id, @Valid @ModelAttribute("formulario") AlergiaForm formulario,
                          BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            pacientesService.agregarAlergia(id, formulario);
            redirect.addFlashAttribute("mensaje", "Alergia agregada correctamente.");
            return "redirect:/pacientes/" + id + "/alergias";
        }
        return prepararAlergias(model, id);
    }

    @PostMapping("/{id}/alergias/{alergiaId}/editar")
    String editarAlergia(@PathVariable Long id, @PathVariable Long alergiaId,
                         @Valid @ModelAttribute("formulario") AlergiaForm formulario,
                         BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            pacientesService.actualizarAlergia(id, alergiaId, formulario);
            redirect.addFlashAttribute("mensaje", "Alergia actualizada correctamente.");
            return "redirect:/pacientes/" + id + "/alergias";
        }
        return prepararAlergias(model, id);
    }

    @PostMapping("/{id}/alergias/{alergiaId}/eliminar")
    String eliminarAlergia(@PathVariable Long id, @PathVariable Long alergiaId, RedirectAttributes redirect) {
        pacientesService.eliminarAlergia(id, alergiaId);
        redirect.addFlashAttribute("mensaje", "Alergia eliminada correctamente.");
        return "redirect:/pacientes/" + id + "/alergias";
    }

    @PostMapping("/{id}/dar-de-alta")
    String darDeAlta(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            PacienteAltaVista alta = pacientesService.darDeAlta(id);
            redirect.addFlashAttribute("mensaje", "Paciente dado de alta correctamente. La cama "
                + alta.cama() + " quedó liberada.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("mensajeError", ex instanceof DataIntegrityViolationException
                ? "No se pudo completar la operación porque la cama ya está ocupada."
                : ex.getMessage());
        }
        return "redirect:/pacientes";
    }

    @PostMapping("/{id}/reingresar")
    String reingresar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            PacienteAltaVista ingreso = pacientesService.reingresar(id);
            redirect.addFlashAttribute("mensaje",
                "Paciente " + ingreso.codigo() + " ingresado y ubicado en camilla "
                    + ingreso.cama() + ".");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/pacientes";
    }

    @GetMapping("/pisos/{pisoId}/camas-disponibles")
    @ResponseBody
    java.util.List<CamaDisponibleVista> camasDisponibles(@PathVariable Long pisoId) {
        return pacientesService.camasDisponibles(pisoId).stream()
            .map(cama -> new CamaDisponibleVista(cama.getId(), cama.getNumero()))
            .toList();
    }

    @GetMapping("/pisos/{pisoId}/camas-disponibles-edicion")
    @ResponseBody
    java.util.List<CamaDisponibleVista> camasDisponiblesEdicion(@PathVariable Long pisoId,
                                                               @RequestParam Long pacienteId) {
        return pacientesService.camasDisponiblesEdicion(pisoId, pacienteId).stream()
            .map(cama -> new CamaDisponibleVista(cama.getId(), cama.getNumero()))
            .toList();
    }

    private String prepararFormulario(Model model) {
        model.addAttribute("pisos", pacientesService.pisosDisponibles());
        model.addAttribute("responsables", pacientesService.responsablesActivos());
        model.addAttribute("hoy", LocalDate.now());
        return "pacientes/formulario";
    }

    private String prepararFormularioEdicion(Model model, Long pacienteId) {
        model.addAttribute("pisos", pacientesService.pisos());
        model.addAttribute("responsables", pacientesService.responsablesActivos());
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("pacienteId", pacienteId);
        model.addAttribute("edicion", true);
        return "pacientes/formulario";
    }

    private String prepararAlergias(Model model, Long pacienteId) {
        model.addAttribute("paciente", pacientesService.consultarPacienteClinico(pacienteId));
        model.addAttribute("alergias", pacientesService.alergias(pacienteId));
        model.addAttribute("severidades", SeveridadAlergia.values());
        model.addAttribute("hoy", LocalDate.now());
        return "pacientes/alergias";
    }

    private String prepararEnfermedades(Model model, Long pacienteId) {
        model.addAttribute("paciente", pacientesService.consultarPacienteClinico(pacienteId));
        model.addAttribute("enfermedades", pacientesService.enfermedades(pacienteId));
        model.addAttribute("hoy", LocalDate.now());
        return "pacientes/enfermedades";
    }

    private String redireccionExito(RedirectAttributes redirect, String mensaje) {
        redirect.addFlashAttribute("mensaje", mensaje);
        return "redirect:/pacientes";
    }

    public record CamaDisponibleVista(Long id, String numero) { }

    public record PacienteAltaVista(String codigo, String cama) { }
}
