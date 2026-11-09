package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import java.util.Set;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

/** HU-02: catálogo de reportes y sus rutas de consulta. */
public enum TipoReporte {
    PACIENTES("Pacientes y ubicación", "Pacientes registrados y su ubicación hospitalaria.", false),
    ASIGNACIONES("Asignaciones de dieta activas", "Dietas y menús asignados a los pacientes.", true),
    MENUS("Menús programados", "Menús por fecha y tiempo de comida.", true),
    OCUPACION("Camas y ocupación", "Disponibilidad y ocupación de camas por piso.", false);

    private static final Set<Rol> ROLES_GENERALES = Set.of(Rol.ROLE_ADMIN, Rol.ROLE_MEDICO,
        Rol.ROLE_NUTRICIONISTA, Rol.ROLE_ENCARGADO_PISO);
    private final String titulo;
    private final String descripcion;
    private final boolean permiteCocina;

    TipoReporte(String titulo, String descripcion, boolean permiteCocina) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.permiteCocina = permiteCocina;
    }

    public String getRuta() {
        return "/reportes/" + name().toLowerCase(java.util.Locale.ROOT);
    }

    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }

    public boolean permite(Rol rol) {
        return rol != null && (ROLES_GENERALES.contains(rol) || (permiteCocina && rol == Rol.ROLE_COCINA));
    }
}
