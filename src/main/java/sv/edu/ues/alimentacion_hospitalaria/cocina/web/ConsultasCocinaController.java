package sv.edu.ues.alimentacion_hospitalaria.cocina.web;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sv.edu.ues.alimentacion_hospitalaria.cocina.service.ConsultasCocinaService;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;

/** SAH-6: consultas operativas de cocina (SC-03.1 y SC-03.2). Solo admite lectura. */
@Controller
@RequestMapping("/cocina/consultas")
public class ConsultasCocinaController {

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");

    private final ConsultasCocinaService consultas;

    public ConsultasCocinaController(ConsultasCocinaService consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/pacientes-por-menu")
    String pacientesPorMenu(@RequestParam(required = false) TipoDieta tipoDieta, Model model) {
        model.addAttribute("regimenes", TipoDieta.values());
        model.addAttribute("regimenSeleccionado", tipoDieta);
        if (tipoDieta != null) {
            model.addAttribute("consulta", consultas.pacientesPorRegimen(tipoDieta, hoy()));
        }
        return "cocina/pacientes-por-menu";
    }

    @GetMapping("/menus-por-paciente")
    String menusPorPaciente(@RequestParam(required = false) Long pacienteId, Model model) {
        model.addAttribute("pacientes", consultas.pacientesInternos());
        model.addAttribute("pacienteSeleccionado", pacienteId);
        if (pacienteId != null) {
            model.addAttribute("consulta", consultas.menusPorPaciente(pacienteId, hoy()));
        }
        return "cocina/menus-por-paciente";
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZONA_EL_SALVADOR);
    }
}
