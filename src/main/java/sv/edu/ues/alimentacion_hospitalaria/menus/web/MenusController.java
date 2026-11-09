package sv.edu.ues.alimentacion_hospitalaria.menus.web;

import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.dto.InsumoDetalleForm;
import sv.edu.ues.alimentacion_hospitalaria.menus.dto.MenuDetalleForm;
import sv.edu.ues.alimentacion_hospitalaria.menus.dto.MenuForm;
import sv.edu.ues.alimentacion_hospitalaria.menus.service.AdministracionMenusService;

/**
 * Rutas alineadas con ReglasAcceso: la consulta (GET /menus, /menus/{id}/detalles) es de los cinco roles;
 * las pantallas de modificación están bajo /menus/nuevo y /menus/{id}/editar/**, y los POST, bajo /menus/**,
 * reservados a Administración y Nutrición.
 */
@Controller
@RequestMapping("/menus")
public class MenusController {

    private final AdministracionMenusService menusService;

    public MenusController(AdministracionMenusService menusService) {
        this.menusService = menusService;
    }

    @InitBinder("formulario")
    void limitarCamposMenu(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "tipo", "tipoDieta");
    }

    @InitBinder("detalle")
    void limitarCamposDetalle(WebDataBinder binder) {
        binder.setAllowedFields("tiempoComida", "horaServicio", "platillo");
    }

    @InitBinder("insumo")
    void limitarCamposInsumo(WebDataBinder binder) {
        binder.setAllowedFields("insumoId", "cantidadPorRacion");
    }

    @GetMapping
    String listar(Model model) {
        model.addAttribute("menus", menusService.listar());
        return "menus/listado";
    }

    @GetMapping("/{id}/detalles")
    String consultar(@PathVariable Long id, Model model) {
        model.addAttribute("menu", menusService.consultar(id));
        return "menus/detalle";
    }

    @GetMapping("/nuevo")
    String nuevo(Model model) {
        model.addAttribute("formulario", new MenuForm());
        return formularioMenu(model, null);
    }

    @PostMapping
    String crear(@Valid @ModelAttribute("formulario") MenuForm formulario, BindingResult errores, Model model,
                 RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            Long id = menusService.crear(formulario);
            redirect.addFlashAttribute("mensaje", "Menú registrado en borrador. Agregue sus tiempos de comida.");
            return "redirect:/menus/" + id + "/detalles";
        }
        return formularioMenu(model, null);
    }

    @GetMapping("/{id}/editar")
    String editar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", menusService.formulario(id));
        return formularioMenu(model, id);
    }

    @PostMapping("/{id}/editar")
    String guardarEdicion(@PathVariable Long id, @Valid @ModelAttribute("formulario") MenuForm formulario,
                          BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            menusService.editar(id, formulario);
            return exito(redirect, id, "Menú actualizado correctamente.");
        }
        return formularioMenu(model, id);
    }

    @PostMapping("/{id}/activar")
    String activar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            menusService.activar(id);
            return exito(redirect, id, "Menú activado. Ya puede asignarse a pacientes.");
        } catch (IllegalArgumentException ex) {
            return error(redirect, id, ex.getMessage());
        }
    }

    @PostMapping("/{id}/desactivar")
    String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        menusService.desactivar(id);
        return exito(redirect, id, "Menú desactivado. Deja de considerarse en cocina y reportes.");
    }

    @GetMapping("/{id}/editar/detalles/nuevo")
    String nuevoDetalle(@PathVariable Long id, Model model) {
        model.addAttribute("menu", menusService.consultar(id));
        model.addAttribute("detalle", new MenuDetalleForm());
        return formularioDetalle(model, id, null);
    }

    @PostMapping("/{id}/editar/detalles")
    String agregarDetalle(@PathVariable Long id, @Valid @ModelAttribute("detalle") MenuDetalleForm detalle,
                          BindingResult errores, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                menusService.agregarDetalle(id, detalle);
                return exito(redirect, id, "Tiempo de comida agregado.");
            } catch (IllegalArgumentException ex) {
                errores.reject("regla", ex.getMessage());
            }
        }
        model.addAttribute("menu", menusService.consultar(id));
        return formularioDetalle(model, id, null);
    }

    @GetMapping("/{id}/editar/detalles/{detalleId}")
    String editarDetalle(@PathVariable Long id, @PathVariable Long detalleId, Model model) {
        model.addAttribute("detalle", menusService.formularioDetalle(id, detalleId));
        return prepararEdicionDetalle(model, id, detalleId);
    }

    @PostMapping("/{id}/editar/detalles/{detalleId}")
    String guardarDetalle(@PathVariable Long id, @PathVariable Long detalleId,
                          @Valid @ModelAttribute("detalle") MenuDetalleForm detalle, BindingResult errores,
                          Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                menusService.editarDetalle(id, detalleId, detalle);
                return exito(redirect, id, "Tiempo de comida actualizado.");
            } catch (IllegalArgumentException ex) {
                errores.reject("regla", ex.getMessage());
            }
        }
        return prepararEdicionDetalle(model, id, detalleId);
    }

    @PostMapping("/{id}/editar/detalles/{detalleId}/eliminar")
    String eliminarDetalle(@PathVariable Long id, @PathVariable Long detalleId, RedirectAttributes redirect) {
        try {
            menusService.eliminarDetalle(id, detalleId);
            return exito(redirect, id, "Tiempo de comida eliminado.");
        } catch (IllegalArgumentException ex) {
            return error(redirect, id, ex.getMessage());
        }
    }

    @PostMapping("/{id}/editar/detalles/{detalleId}/insumos")
    String guardarInsumo(@PathVariable Long id, @PathVariable Long detalleId,
                         @Valid @ModelAttribute("insumo") InsumoDetalleForm insumo, BindingResult errores,
                         RedirectAttributes redirect) {
        String destino = "redirect:/menus/" + id + "/editar/detalles/" + detalleId;
        if (errores.hasErrors()) {
            redirect.addFlashAttribute("error", errores.getAllErrors().getFirst().getDefaultMessage());
            return destino;
        }
        try {
            menusService.guardarInsumo(id, detalleId, insumo);
            redirect.addFlashAttribute("mensaje", "Insumo guardado en el tiempo de comida.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return destino;
    }

    @PostMapping("/{id}/editar/detalles/{detalleId}/insumos/{insumoId}/eliminar")
    String quitarInsumo(@PathVariable Long id, @PathVariable Long detalleId, @PathVariable Long insumoId,
                        RedirectAttributes redirect) {
        menusService.quitarInsumo(id, detalleId, insumoId);
        redirect.addFlashAttribute("mensaje", "Insumo quitado del tiempo de comida.");
        return "redirect:/menus/" + id + "/editar/detalles/" + detalleId;
    }

    private String prepararEdicionDetalle(Model model, Long id, Long detalleId) {
        model.addAttribute("menu", menusService.consultar(id));
        model.addAttribute("detalleActual", menusService.consultarDetalle(id, detalleId));
        model.addAttribute("insumos", menusService.opcionesInsumo());
        if (!model.containsAttribute("insumo")) {
            model.addAttribute("insumo", new InsumoDetalleForm());
        }
        return formularioDetalle(model, id, detalleId);
    }

    private String formularioMenu(Model model, Long id) {
        model.addAttribute("creacion", id == null);
        model.addAttribute("accion", id == null ? "/menus" : "/menus/" + id + "/editar");
        model.addAttribute("tipos", TipoMenu.values());
        model.addAttribute("tiposDieta", TipoDieta.values());
        return "menus/formulario";
    }

    private String formularioDetalle(Model model, Long id, Long detalleId) {
        model.addAttribute("creacion", detalleId == null);
        model.addAttribute("accion", detalleId == null
            ? "/menus/" + id + "/editar/detalles" : "/menus/" + id + "/editar/detalles/" + detalleId);
        model.addAttribute("tiemposComida", TiempoComida.values());
        return "menus/detalle-formulario";
    }

    private String exito(RedirectAttributes redirect, Long id, String mensaje) {
        redirect.addFlashAttribute("mensaje", mensaje);
        return "redirect:/menus/" + id + "/detalles";
    }

    private String error(RedirectAttributes redirect, Long id, String mensaje) {
        redirect.addFlashAttribute("error", mensaje);
        return "redirect:/menus/" + id + "/detalles";
    }
}
