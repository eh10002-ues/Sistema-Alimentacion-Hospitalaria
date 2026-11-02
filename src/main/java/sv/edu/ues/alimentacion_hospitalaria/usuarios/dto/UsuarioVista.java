package sv.edu.ues.alimentacion_hospitalaria.usuarios.dto;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

public record UsuarioVista(Long id, String nombreUsuario, String nombreCompleto, Rol rol, boolean activo) {
	public static UsuarioVista desde(Usuario usuario) {
		return new UsuarioVista(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombreCompleto(),
			usuario.getRol(), usuario.isActivo());
	}
}
