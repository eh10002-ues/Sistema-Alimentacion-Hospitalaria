package sv.edu.ues.alimentacion_hospitalaria.usuarios.service;

import java.util.Locale;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.security.RevocadorSesiones;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.*;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.dto.*;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdministracionUsuariosService {
	private final UsuarioRepository usuarios;
	private final ServicioContrasenas contrasenas;
	private final RevocadorSesiones sesiones;
	private final Validator validator;

	public AdministracionUsuariosService(UsuarioRepository usuarios, ServicioContrasenas contrasenas,
		RevocadorSesiones sesiones, Validator validator) {
		this.usuarios = usuarios;
		this.contrasenas = contrasenas;
		this.sesiones = sesiones;
		this.validator = validator;
	}

	public Page<UsuarioVista> listar(int pagina) {
		return usuarios.findAll(PageRequest.of(Math.max(0, pagina), 20, Sort.by("nombreUsuario", "id")))
			.map(UsuarioVista::desde);
	}

	public UsuarioVista consultar(Long id) {
		return UsuarioVista.desde(buscar(id));
	}

	@Transactional
	public void crear(CrearUsuarioForm formulario) {
		bloquearYVerificarActor();
		validar(formulario);
		String nombre = formulario.getNombreUsuario().toLowerCase(Locale.ROOT);
		comprobarNombreDisponible(nombre, null);
		String hash = contrasenas.codificar(nombre, formulario.getContrasena(), formulario.getConfirmacion());
		usuarios.save(new Usuario(nombre, hash, formulario.getNombreCompleto(), formulario.getRol(), formulario.isActivo()));
	}

	@Transactional
	public void editar(Long id, EditarUsuarioForm formulario) {
		bloquearYVerificarActor();
		validar(formulario);
		Usuario usuario = buscar(id);
		String nombreAnterior = usuario.getNombreUsuario();
		String nombre = formulario.getNombreUsuario().toLowerCase(Locale.ROOT);
		comprobarNombreDisponible(nombre, id);
		if (usuario.getRol() != formulario.getRol()) {
			protegerUltimoAdministrador(usuario);
			sesiones.despuesDeConfirmar(nombreAnterior);
		} else if (!nombreAnterior.equals(nombre)) {
			sesiones.despuesDeConfirmar(nombreAnterior);
		}
		usuario.setNombreUsuario(nombre);
		usuario.setNombreCompleto(formulario.getNombreCompleto());
		usuario.setRol(formulario.getRol());
	}

	@Transactional
	public void cambiarEstado(Long id, boolean activo) {
		Usuario actor = bloquearYVerificarActor();
		Usuario usuario = buscar(id);
		if (!activo) {
			if (actor.getId().equals(id)) {
				throw new IllegalArgumentException("No puede desactivar su propia cuenta.");
			}
			protegerUltimoAdministrador(usuario);
			sesiones.despuesDeConfirmar(usuario.getNombreUsuario());
		}
		usuario.setActivo(activo);
	}

	@Transactional
	public void restablecer(Long id, RestablecerContrasenaForm formulario) {
		bloquearYVerificarActor();
		Usuario usuario = buscar(id);
		usuario.setContrasenaHash(contrasenas.codificar(usuario.getNombreUsuario(),
			formulario.getContrasena(), formulario.getConfirmacion()));
		sesiones.despuesDeConfirmar(usuario.getNombreUsuario());
	}

	private Usuario bloquearYVerificarActor() {
		usuarios.bloquearAdministracion();
		Usuario actor = usuarios.findByNombreUsuarioIgnoreCase(
			SecurityContextHolder.getContext().getAuthentication().getName())
			.orElseThrow(() -> new AccessDeniedException("Acceso denegado."));
		if (!actor.isActivo() || actor.getRol() != Rol.ROLE_ADMIN) {
			throw new AccessDeniedException("Acceso denegado.");
		}
		return actor;
	}

	private Usuario buscar(Long id) {
		return usuarios.findById(id).orElseThrow(() -> new ResourceNotFoundException("El usuario solicitado no existe."));
	}

	private void comprobarNombreDisponible(String nombre, Long id) {
		usuarios.findByNombreUsuarioIgnoreCase(nombre).ifPresent(existente -> {
			if (!existente.getId().equals(id)) {
				throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
			}
		});
	}

	private void protegerUltimoAdministrador(Usuario usuario) {
		if (usuario.isActivo() && usuario.getRol() == Rol.ROLE_ADMIN
			&& usuarios.countByRolAndActivoTrue(Rol.ROLE_ADMIN) <= 1) {
			throw new IllegalArgumentException("No puede retirar el acceso del último administrador activo.");
		}
	}

	private void validar(Object formulario) {
		if (!validator.validate(formulario).isEmpty()) {
			throw new IllegalArgumentException("Revise los datos del usuario y seleccione un rol válido.");
		}
	}
}
