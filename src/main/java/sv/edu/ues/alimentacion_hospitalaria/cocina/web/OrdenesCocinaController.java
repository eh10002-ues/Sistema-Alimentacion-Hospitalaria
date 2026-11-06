package sv.edu.ues.alimentacion_hospitalaria.cocina.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import sv.edu.ues.alimentacion_hospitalaria.cocina.service.NotificadorCocina;

/**
 * SAH-2: recepción mínima de las órdenes aprobadas. Solo muestra lo que llega mientras
 * la página está abierta; las consultas operativas de cocina corresponden a SAH-6.
 */
@Controller
@RequestMapping("/cocina/ordenes")
public class OrdenesCocinaController {

    private final NotificadorCocina notificador;

    public OrdenesCocinaController(NotificadorCocina notificador) {
        this.notificador = notificador;
    }

    @GetMapping
    String ordenes() {
        return "cocina/ordenes";
    }

    @GetMapping(path = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    SseEmitter eventos() {
        return notificador.suscribir();
    }
}
