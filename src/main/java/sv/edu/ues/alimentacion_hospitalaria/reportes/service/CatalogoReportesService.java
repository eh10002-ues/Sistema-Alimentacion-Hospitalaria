package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.OpcionReporteVista;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.TipoReporte;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilasReporte.OpcionPiso;
import sv.edu.ues.alimentacion_hospitalaria.reportes.persistence.ConsultasReportesRepository;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'NUTRICIONISTA', 'ENCARGADO_PISO', 'COCINA')")
public class CatalogoReportesService {
    private final AccesoReportes acceso;
    private final ConsultasReportesRepository consultas;

    public CatalogoReportesService(AccesoReportes acceso, ConsultasReportesRepository consultas) {
        this.acceso = acceso;
        this.consultas = consultas;
    }

    public List<OpcionPiso> pisos() {
        acceso.rolActual();
        return consultas.pisos().stream().map(fila -> new OpcionPiso(fila.get(0, Long.class),
            fila.get(1, String.class), fila.get(2, String.class))).toList();
    }

    public List<OpcionReporteVista> consultar() {
        Rol rol = acceso.rolActual();
        return Arrays.stream(TipoReporte.values())
            .filter(tipo -> tipo.permite(rol))
            .map(OpcionReporteVista::desde)
            .toList();
    }

}
