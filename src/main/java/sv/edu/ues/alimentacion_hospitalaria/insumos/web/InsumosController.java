package sv.edu.ues.alimentacion_hospitalaria.insumos.web;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.dao.DataIntegrityViolationException;
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
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.PeriodoProyeccion;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.InsumoForm;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.ProyeccionInsumosForm;
import sv.edu.ues.alimentacion_hospitalaria.insumos.service.AdministracionInsumosService;
import sv.edu.ues.alimentacion_hospitalaria.insumos.service.ProyeccionInsumosService;

@Controller
@RequestMapping("/insumos")
public class InsumosController {

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");

    private final AdministracionInsumosService administracion;
    private final ProyeccionInsumosService proyeccionService;

    public InsumosController(AdministracionInsumosService administracion, ProyeccionInsumosService proyeccionService) {
        this.administracion = administracion;
        this.proyeccionService = proyeccionService;
    }

    @InitBinder("formulario")
    void limitarCampos(WebDataBinder binder) {
        binder.setAllowedFields(
            "periodo", "margenFraccion", "nombre", "unidad", "mermaPorcentaje",
            "presentacion", "cantidadPresentacion", "costoUnitario");
    }

    @GetMapping
    String listar(@RequestParam(defaultValue = "0") int pagina,
                  @RequestParam(defaultValue = "") String buscar,
                  Model model) {
        model.addAttribute("pagina", administracion.listar(pagina, buscar));
        model.addAttribute("buscar", buscar);
        return "insumos/listado";
    }

    @GetMapping("/nuevo")
    String nuevo(Model model) {
        model.addAttribute("formulario", new InsumoForm());
        return prepararFormulario(model, null);
    }

    @PostMapping
    String crear(@Valid @ModelAttribute("formulario") InsumoForm formulario,
                 BindingResult errores,
                 Model model,
                 RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                administracion.crear(formulario);
                return exito(redirect, "Insumo registrado correctamente.");
            } catch (IllegalArgumentException ex) {
                errores.reject("regla", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errores.reject("regla", "Ya existe un insumo con ese nombre.");
            }
        }
        return prepararFormulario(model, null);
    }

    @GetMapping("/{id}/editar")
    String editar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", administracion.consultar(id));
        return prepararFormulario(model, id);
    }

    @PostMapping("/{id}")
    String guardarEdicion(@PathVariable Long id,
                          @Valid @ModelAttribute("formulario") InsumoForm formulario,
                          BindingResult errores,
                          Model model,
                          RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                administracion.editar(id, formulario);
                return exito(redirect, "Insumo actualizado correctamente.");
            } catch (IllegalArgumentException ex) {
                errores.reject("regla", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errores.reject("regla", "Ya existe un insumo con ese nombre.");
            }
        }
        return prepararFormulario(model, id);
    }

    @PostMapping("/{id}/eliminar")
    String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            administracion.eliminar(id);
            return exito(redirect, "Insumo eliminado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/insumos";
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("error",
                "No se puede eliminar el insumo porque está asociado a uno o más menús.");
            return "redirect:/insumos";
        }
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

    private String prepararFormulario(Model model, Long id) {
        model.addAttribute("creacion", id == null);
        model.addAttribute("accion", id == null ? "/insumos" : "/insumos/" + id);
        return "insumos/formulario";
    }

    private String exito(RedirectAttributes redirect, String mensaje) {
        redirect.addFlashAttribute("mensaje", mensaje);
        return "redirect:/insumos";
    }
}
