package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto;

public record PisoVista(
    Long id,
    String codigo,
    String nombre,
    long encargados,
    long camas
) {}
