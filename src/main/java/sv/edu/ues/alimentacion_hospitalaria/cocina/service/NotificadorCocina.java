package sv.edu.ues.alimentacion_hospitalaria.cocina.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import sv.edu.ues.alimentacion_hospitalaria.asignaciones.service.AsignacionMenuAprobada;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.service.PrescripcionAutorizada;

/**
 * SAH-2: envía a cocina, mediante Server-Sent Events, cada prescripción aprobada.
 * D4: envía también cada asignación de menú aprobada y vigente, con un evento distinto.
 * Las conexiones viven en memoria (una instancia de la aplicación). Expiran cada pocos
 * minutos para que el navegador se reconecte y la autorización se vuelva a comprobar.
 */
@Component
public class NotificadorCocina {

    static final String EVENTO_CONEXION = "conectado";
    static final String EVENTO_ORDEN = "prescripcion-aprobada";
    static final String EVENTO_MENU = "asignacion-menu-aprobada";
    private static final long DURACION_CONEXION_MS = Duration.ofMinutes(5).toMillis();

    private final List<SseEmitter> conexiones = new CopyOnWriteArrayList<>();

    public SseEmitter suscribir() {
        SseEmitter conexion = new SseEmitter(DURACION_CONEXION_MS);
        conexion.onCompletion(() -> conexiones.remove(conexion));
        conexion.onTimeout(() -> {
            conexiones.remove(conexion);
            conexion.complete();
        });
        conexion.onError(error -> conexiones.remove(conexion));
        conexiones.add(conexion);
        try {
            // Confirma la conexión al navegador antes de que llegue la primera orden.
            conexion.send(SseEmitter.event().name(EVENTO_CONEXION).data("ok"));
        } catch (IOException ex) {
            conexiones.remove(conexion);
            conexion.completeWithError(ex);
        }
        return conexion;
    }

    /** Solo se ejecuta si la autorización se confirmó; una transacción revertida no notifica. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(PrescripcionAutorizada evento) {
        difundir(() -> SseEmitter.event().name(EVENTO_ORDEN)
            .id(String.valueOf(evento.orden().prescripcionId())).data(evento.orden()));
    }

    /** D4: asignación de menú aprobada y vigente; usa su propio evento para no mezclarla con SAH-2. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(AsignacionMenuAprobada evento) {
        difundir(() -> SseEmitter.event().name(EVENTO_MENU)
            .id("asignacion-" + evento.orden().asignacionId()).data(evento.orden()));
    }

    // Un constructor de evento nuevo por conexión: SseEventBuilder no es reutilizable.
    private void difundir(Supplier<SseEmitter.SseEventBuilder> evento) {
        for (SseEmitter conexion : conexiones) {
            try {
                conexion.send(evento.get());
            } catch (IOException | IllegalStateException ex) {
                // Conexión cerrada por el navegador: se descarta sin afectar al resto.
                conexiones.remove(conexion);
            }
        }
    }
}
