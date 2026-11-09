package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Turno;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.AsignacionPisoForm;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.CamaForm;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.PisoForm;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.service.AdministracionUbicacionesService;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
@RequestMapping("/ubicaciones")
public class UbicacionesController {

    private final AdministracionUbicacionesService ubicaciones;

    public UbicacionesController(AdministracionUbicacionesService ubicaciones) {
        this.ubicaciones = ubicaciones;
    }

    @GetMapping
    String listar(@RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "") String buscar, Model model) {
        model.addAttribute("pagina", ubicaciones.listar(pagina, buscar));
        model.addAttribute("buscar", buscar);
        return "ubicaciones/listado";
    }

    @GetMapping("/{id}")
    String ver(@PathVariable Long id, Model model) {
        model.addAttribute("detalle", ubicaciones.consultarDetalle(id));
        return "ubicaciones/detalle";
    }

    @GetMapping("/nuevo")
    String nuevo(Model model) {
        model.addAttribute("formulario", new PisoForm());
        return prepararPiso(model, null);
    }

    @PostMapping
    String crear(@Valid @ModelAttribute("formulario") PisoForm formulario, BindingResult errores,
        Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                ubicaciones.crear(formulario);
                return exito(redirect, "Piso creado correctamente.");
            } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                errores.reject("regla", mensajeError(ex));
            }
        }
        return prepararPiso(model, null);
    }

    @GetMapping("/{id}/editar")
    String editar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", ubicaciones.consultarPiso(id));
        return prepararPiso(model, id);
    }

    @PostMapping("/{id}")
    String guardarEdicion(@PathVariable Long id, @Valid @ModelAttribute("formulario") PisoForm formulario,
        BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                ubicaciones.editar(id, formulario);
                return exito(redirect, "Piso actualizado correctamente.");
            } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                errores.reject("regla", mensajeError(ex));
            }
        }
        return prepararPiso(model, id);
    }

    @GetMapping("/{id}/encargados")
    String asignarEncargados(@PathVariable Long id, Model model) {
        AsignacionPisoForm formulario = ubicaciones.consultarEncargados(id);
        model.addAttribute("formulario", formulario);
        model.addAttribute("piso", ubicaciones.buscarPiso(id));
        model.addAttribute("encargados", ubicaciones.encargadosActivos());
        model.addAttribute("turnos", Turno.values());
        return "ubicaciones/asignar-encargados";
    }

    @PostMapping("/{id}/encargados")
    String guardarEncargados(@PathVariable Long id, @Valid @ModelAttribute("formulario") AsignacionPisoForm formulario,
        BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                ubicaciones.asignarEncargados(id, formulario);
                return exito(redirect, "Encargados asignados correctamente.");
            } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                errores.reject("regla", mensajeError(ex));
            }
        }
        model.addAttribute("piso", ubicaciones.buscarPiso(id));
        model.addAttribute("encargados", ubicaciones.encargadosActivos());
        model.addAttribute("turnos", Turno.values());
        return "ubicaciones/asignar-encargados";
    }

    @GetMapping("/{id}/camas/nueva")
    String nuevaCama(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", ubicaciones.consultarCamas(id));
        model.addAttribute("piso", ubicaciones.buscarPiso(id));
        return "ubicaciones/cama-formulario";
    }

    @PostMapping("/{id}/camas")
    String crearCamas(@PathVariable Long id, @Valid @ModelAttribute("formulario") CamaForm formulario,
        BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                ubicaciones.crearCamas(id, formulario);
                return exito(redirect, "Camas registradas correctamente.");
            } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                errores.reject("regla", mensajeError(ex));
            }
        }
        model.addAttribute("piso", ubicaciones.buscarPiso(id));
        return "ubicaciones/cama-formulario";
    }

    private String prepararPiso(Model model, Long id) {
        model.addAttribute("creacion", id == null);
        model.addAttribute("accion", id == null ? "/ubicaciones" : "/ubicaciones/" + id);
        return "ubicaciones/piso-formulario";
    }

    private String exito(RedirectAttributes redirect, String mensaje) {
        redirect.addFlashAttribute("mensaje", mensaje);
        return "redirect:/ubicaciones";
    }

    private String mensajeError(Exception ex) {
        if (ex instanceof DataIntegrityViolationException) {
            return "Los datos coinciden con un registro existente. Revise piso, cama, encargado y turno.";
        }
        return ex.getMessage();
    }
}
