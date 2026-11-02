package sv.edu.ues.alimentacion_hospitalaria.inicio;

import java.util.List;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

public record PanelRol(String usuario, Rol rol, String titulo, String descripcion,
	List<String> funciones, List<Acceso> accesos) {
	public record Acceso(String titulo, String ruta) { }
}
