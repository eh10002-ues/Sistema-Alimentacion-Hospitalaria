package sv.edu.ues.alimentacion_hospitalaria.usuarios.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ServicioContrasenas {

	private final PoliticaContrasena politica;
	private final PasswordEncoder encoder;

	public ServicioContrasenas(PoliticaContrasena politica, PasswordEncoder encoder) {
		this.politica = politica;
		this.encoder = encoder;
	}

	public String codificar(String nombreUsuario, String contrasena, String confirmacion) {
		politica.validar(nombreUsuario, contrasena, confirmacion);
		return encoder.encode(contrasena);
	}
}
