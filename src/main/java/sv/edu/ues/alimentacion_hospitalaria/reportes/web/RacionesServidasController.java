package sv.edu.ues.alimentacion_hospitalaria.reportes.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.RacionesServidasService;

@Controller
@RequestMapping("/reportes/raciones-servidas")
public class RacionesServidasController {

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");

    private final RacionesServidasService reportes;

    public RacionesServidasController(RacionesServidasService reportes) {
        this.reportes = reportes;
    }

    @GetMapping
    String racionesServidas(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
        Model model) {
        LocalDate hoy = LocalDate.now(ZONA_EL_SALVADOR);
        YearMonth mes = periodo == null ? YearMonth.from(hoy) : periodo;
        if (mes.isAfter(YearMonth.from(hoy))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El reporte no admite meses futuros.");
        }
        model.addAttribute("consulta", reportes.generar(mes, hoy));
        model.addAttribute("mesSeleccionado", mes);
        model.addAttribute("mesActual", YearMonth.from(hoy));
        return "reportes/raciones-servidas";
    }
}
