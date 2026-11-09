package sv.edu.ues.alimentacion_hospitalaria.menus.domain;

/**
 * La aprobación médica vive en AsignacionMenu, por eso el estado del menú
 * solo indica su disponibilidad. Ajustar si el equipo define otros valores.
 */
public enum EstadoMenu {
    BORRADOR,
    ACTIVO,
    INACTIVO
}
