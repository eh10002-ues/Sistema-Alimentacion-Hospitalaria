package sv.edu.ues.alimentacion_hospitalaria.usuarios.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestablecerContrasenaForm {
	private String contrasena;
	private String confirmacion;

	public void limpiarSecretos() {
		contrasena = null;
		confirmacion = null;
	}
}
