package sv.edu.ues.alimentacion_hospitalaria.insumos.service;

import org.springframework.stereotype.Service;
import sv.edu.ues.alimentacion_hospitalaria.insumos.persistence.InsumoRepository;

@Service
public class AdministracionInsumosService {

    private final InsumoRepository insumoRepository;

    public AdministracionInsumosService(InsumoRepository insumoRepository) {
        this.insumoRepository = insumoRepository;
    }


}
