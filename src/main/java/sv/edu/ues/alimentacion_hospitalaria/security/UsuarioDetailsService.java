package sv.edu.ues.alimentacion_hospitalaria.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarios;

	public UsuarioDetailsService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	public UserDetails loadUserByUsername(String nombreUsuario) {
		if (nombreUsuario == null || nombreUsuario.isBlank()
			|| nombreUsuario.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
			throw new UsernameNotFoundException("Usuario o contraseña incorrectos.");
		}
		var usuario = usuarios.findByNombreUsuarioIgnoreCase(nombreUsuario)
			.orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos."));
		return User.withUsername(usuario.getNombreUsuario())
			.password(usuario.getContrasenaHash())
			.authorities(usuario.getRol().name())
			.disabled(!usuario.isActivo())
			.build();
	}
}
