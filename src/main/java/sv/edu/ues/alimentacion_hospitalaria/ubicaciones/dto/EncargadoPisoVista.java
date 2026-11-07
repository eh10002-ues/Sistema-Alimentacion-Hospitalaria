package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto;

import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Turno;

public record EncargadoPisoVista(
    Long id,
    String nombreUsuario,
    String nombreCompleto,
    Turno turno
) {}
