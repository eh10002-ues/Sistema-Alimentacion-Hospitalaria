package sv.edu.ues.alimentacion_hospitalaria.cocina.web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto.OrdenMenuCocina;
import sv.edu.ues.alimentacion_hospitalaria.cocina.service.NotificadorCocina;
import sv.edu.ues.alimentacion_hospitalaria.cocina.service.OrdenesCocinaService;

/**
 * SAH-2: recepción en tiempo real de las prescripciones aprobadas (solo mientras la página está abierta).
 * D4: además, los menús aprobados y vigentes hoy se cargan al abrir la página, se actualizan por SSE
 * al aprobarse una asignación y se vuelven a consultar en cada reconexión del canal.
 */
@Controller
@RequestMapping("/cocina/ordenes")
public class OrdenesCocinaController {

    private static final ZoneId ZONA_EL_SALVADOR = ZoneId.of("America/El_Salvador");

    private final NotificadorCocina notificador;
    private final OrdenesCocinaService ordenes;

    public OrdenesCocinaController(NotificadorCocina notificador, OrdenesCocinaService ordenes) {
        this.notificador = notificador;
        this.ordenes = ordenes;
    }

    @GetMapping
    String ordenes(Model model) {
        model.addAttribute("menusAprobados", ordenes.menusAprobadosVigentes(LocalDate.now(ZONA_EL_SALVADOR)));
        return "cocina/ordenes";
    }

    @GetMapping(path = "/menus", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    List<OrdenMenuCocina> menusAprobados() {
        return ordenes.menusAprobadosVigentes(LocalDate.now(ZONA_EL_SALVADOR));
    }

    @GetMapping(path = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    SseEmitter eventos() {
        return notificador.suscribir();
    }
}
