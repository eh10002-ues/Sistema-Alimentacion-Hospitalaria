package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

import jakarta.validation.Validator;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;

/** HU-02: valida las entradas antes de ejecutar consultas paginadas. */
@Component
public class ValidacionConsultaReportes {
    private final Validator validator;
    private final PisoRepository pisos;

    public ValidacionConsultaReportes(Validator validator, PisoRepository pisos) {
        this.validator = validator;
        this.pisos = pisos;
    }

    public void validar(Object filtros, Long pisoId) {
        if (filtros == null) {
            throw new FiltroReporteInvalidoException("Indique los filtros del reporte.");
        }
        var errores = validator.validate(filtros);
        if (!errores.isEmpty()) {
            String mensaje = errores.stream().map(error -> error.getMessage())
                .distinct().sorted().collect(Collectors.joining(" "));
            throw new FiltroReporteInvalidoException(mensaje);
        }
        if (pisoId != null && !pisos.existsById(pisoId)) {
            throw new FiltroReporteInvalidoException("El piso seleccionado no existe.");
        }
    }

    public PageRequest pagina(int pagina) {
        if (pagina < 0) {
            throw new FiltroReporteInvalidoException("La página no puede ser negativa.");
        }
        return PageRequest.of(pagina, FiltrosReporte.TAMANO_PAGINA);
    }
}
