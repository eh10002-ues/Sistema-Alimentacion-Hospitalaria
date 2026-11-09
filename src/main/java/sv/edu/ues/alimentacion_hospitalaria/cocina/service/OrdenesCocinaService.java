package sv.edu.ues.alimentacion_hospitalaria.cocina.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto.OrdenMenuCocina;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;

/**
 * Carga inicial de la pantalla de órdenes: asignaciones de menú aprobadas, activas y vigentes
 * en la fecha, de pacientes ingresados y menús activos (los mismos filtros de SAH-6).
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('COCINA')")
public class OrdenesCocinaService {

    private final AsignacionMenuRepository asignaciones;

    public OrdenesCocinaService(AsignacionMenuRepository asignaciones) {
        this.asignaciones = asignaciones;
    }

    public List<OrdenMenuCocina> menusAprobadosVigentes(LocalDate fecha) {
        return asignaciones.findAprobadasVigentes(EstadoMenu.ACTIVO, EstadoPaciente.INGRESADO, fecha).stream()
            .map(OrdenMenuCocina::desde)
            .toList();
    }
}
