package sv.edu.ues.alimentacion_hospitalaria.usuarios.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

@Getter
@Setter
public class EditarUsuarioForm {
	@NotBlank(message = "Ingrese el nombre de usuario.")
	@Size(max = 100, message = "El nombre de usuario admite hasta 100 caracteres.")
	@Pattern(regexp = "[^\\s\\p{Z}]+", message = "El nombre de usuario no admite espacios.")
	private String nombreUsuario;
	@NotBlank(message = "Ingrese el nombre completo.")
	@Size(max = 150, message = "El nombre completo admite hasta 150 caracteres.")
	private String nombreCompleto;
	@NotNull(message = "Seleccione un rol válido.")
	private Rol rol;

	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario == null ? null : nombreUsuario.toLowerCase(java.util.Locale.ROOT);
	}
}
