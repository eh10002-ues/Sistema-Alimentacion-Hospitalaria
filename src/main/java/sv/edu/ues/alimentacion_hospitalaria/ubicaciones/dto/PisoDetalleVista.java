package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto;

import java.util.List;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;

public record PisoDetalleVista(
    Piso piso,
    List<EncargadoPisoVista> encargados,
    List<Cama> camas
) {}
