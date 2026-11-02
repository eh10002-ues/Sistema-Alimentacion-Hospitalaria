package sv.edu.ues.alimentacion_hospitalaria.usuarios.service;

import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@Service
public class PrimerAdministradorService {

	private final UsuarioRepository usuarios;
	private final ServicioContrasenas contrasenas;
	private final Validator validator;

	public PrimerAdministradorService(UsuarioRepository usuarios, ServicioContrasenas contrasenas, Validator validator) {
		this.usuarios = usuarios;
		this.contrasenas = contrasenas;
		this.validator = validator;
	}

	@Transactional
	public void crearSiNoExiste(String nombreUsuario, String contrasena, String nombreCompleto) {
		usuarios.bloquearAdministracion();
		if (usuarios.existsByRol(Rol.ROLE_ADMIN)) {
			return;
		}
		Usuario administrador = new Usuario(nombreUsuario, null, nombreCompleto, Rol.ROLE_ADMIN, true);
		if (!validator.validateProperty(administrador, "nombreUsuario").isEmpty()) {
			throw new IllegalStateException("Revise APP_ADMIN_USERNAME: es obligatorio, sin espacios y de hasta 100 caracteres.");
		}
		if (!validator.validateProperty(administrador, "nombreCompleto").isEmpty()) {
			throw new IllegalStateException("Revise APP_ADMIN_FULL_NAME: es obligatorio y de hasta 150 caracteres.");
		}
		if (usuarios.existsByNombreUsuarioIgnoreCase(administrador.getNombreUsuario())) {
			throw new IllegalStateException("APP_ADMIN_USERNAME ya está registrado. La inicialización no modifica cuentas existentes.");
		}
		try {
			administrador.setContrasenaHash(contrasenas.codificar(administrador.getNombreUsuario(), contrasena, contrasena));
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Revise APP_ADMIN_PASSWORD: es obligatoria y debe cumplir la política de contraseñas.");
		}
		usuarios.save(administrador);
	}
}
