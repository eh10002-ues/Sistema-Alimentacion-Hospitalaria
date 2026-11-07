package sv.edu.ues.alimentacion_hospitalaria.pacientes.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.service.AdministracionPacientesService;

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
                  Model model) {
        model.addAttribute("pagina", pacientesService.listar(pagina, buscar, estado));
        model.addAttribute("buscar", buscar);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estados", EstadoPaciente.values());
        return "pacientes/listado";
    }

    @GetMapping("/{id}")
    String ver(@PathVariable Long id, Model model) {
        model.addAttribute("paciente", pacientesService.consultar(id));
        return "pacientes/detalle";
    }
}
