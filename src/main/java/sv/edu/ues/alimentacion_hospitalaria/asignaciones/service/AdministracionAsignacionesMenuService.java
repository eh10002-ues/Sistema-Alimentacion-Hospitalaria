package sv.edu.ues.alimentacion_hospitalaria.asignaciones.service;

import org.springframework.stereotype.Service;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;

@Service
public class AdministracionAsignacionesMenuService {

    private final AsignacionMenuRepository asignacionMenuRepository;

    public AdministracionAsignacionesMenuService(AsignacionMenuRepository asignacionMenuRepository) {
        this.asignacionMenuRepository = asignacionMenuRepository;
    }

}
