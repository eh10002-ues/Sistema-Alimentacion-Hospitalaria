package sv.edu.ues.alimentacion_hospitalaria.reportes.web;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.ReportesService;

/** SAH-9: reportes específicos. Solo admite lectura; el PDF se genera en el navegador. */
@Controller
@RequestMapping("/reportes")
public class ReportesController {

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");

    private final ReportesService reportes;

    public ReportesController(ReportesService reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/alergias-severas")
    String alergiasSeveras(@RequestParam(required = false) Long pisoId, Model model) {
        model.addAttribute("reporte", reportes.alergiasSeveras(pisoId));
        filtroPiso(model, pisoId);
        return "reportes/alergias-severas";
    }

    @GetMapping("/comandas")
    String comandas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                    @RequestParam(required = false) Long pisoId,
                    @RequestParam(required = false) TiempoComida tiempoComida, Model model) {
        LocalDate dia = fecha == null ? hoy() : fecha;
        model.addAttribute("reporte", reportes.comandas(dia, pisoId, tiempoComida));
        model.addAttribute("fechaSeleccionada", dia);
        model.addAttribute("tiempos", TiempoComida.values());
        model.addAttribute("tiempoSeleccionado", tiempoComida);
        filtroPiso(model, pisoId);
        return "reportes/comandas";
    }

    @GetMapping("/asignaciones-pendientes")
    String asignacionesPendientes(Model model) {
        model.addAttribute("reporte", reportes.asignacionesPendientes());
        return "reportes/asignaciones-pendientes";
    }

    @GetMapping("/pacientes-sin-menu")
    String pacientesSinMenu(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                            @RequestParam(required = false) Long pisoId, Model model) {
        LocalDate dia = fecha == null ? hoy() : fecha;
        model.addAttribute("reporte", reportes.pacientesSinMenu(dia, pisoId));
        model.addAttribute("fechaSeleccionada", dia);
        filtroPiso(model, pisoId);
        return "reportes/pacientes-sin-menu";
    }

    @GetMapping("/expediente-paciente")
    String expedientePaciente(@RequestParam(required = false) String codigoPaciente,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                              @RequestParam(required = false) String estadoAsignacion, Model model) {
        model.addAttribute("codigoPaciente", codigoPaciente);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        model.addAttribute("estadoAsignacion", estadoAsignacion);
        if (codigoPaciente != null && !codigoPaciente.isBlank()) {
            model.addAttribute("reporte", reportes.expedientePaciente(codigoPaciente.trim(), fechaDesde, fechaHasta,
                estadoAsignacion));
        }
        return "reportes/expediente-paciente";
    }

    private void filtroPiso(Model model, Long pisoId) {
        model.addAttribute("pisos", reportes.pisos());
        model.addAttribute("pisoSeleccionado", pisoId);
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZONA_EL_SALVADOR);
    }
}
