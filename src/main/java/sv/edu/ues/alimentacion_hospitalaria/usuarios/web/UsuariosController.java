package sv.edu.ues.alimentacion_hospitalaria.usuarios.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.dto.*;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.AdministracionUsuariosService;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.PoliticaContrasena;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuariosController {
	private final AdministracionUsuariosService usuarios;
	private final PoliticaContrasena politica;

	public UsuariosController(AdministracionUsuariosService usuarios, PoliticaContrasena politica) {
		this.usuarios = usuarios;
		this.politica = politica;
	}

	@ModelAttribute("politicaContrasena")
	String describirPolitica() {
		return politica.descripcion();
	}

	@InitBinder("formulario")
	void limitarCampos(WebDataBinder binder) {
		// El binder puede configurarse antes de construir el DTO. Cada DTO define sus campos editables.
		binder.setAllowedFields("nombreUsuario", "nombreCompleto", "rol", "activo", "contrasena", "confirmacion");
	}

	@GetMapping
	String listar(@RequestParam(defaultValue = "0") int pagina, Model model) {
		model.addAttribute("pagina", usuarios.listar(pagina));
		return "usuarios/listado";
	}

	@GetMapping("/nuevo")
	String nuevo(Model model) {
		model.addAttribute("formulario", new CrearUsuarioForm());
		return prepararFormulario(model, null);
	}

	@PostMapping
	String crear(@Valid @ModelAttribute("formulario") CrearUsuarioForm formulario, BindingResult errores,
		Model model, RedirectAttributes redirect) {
		try {
			if (!errores.hasErrors()) {
				usuarios.crear(formulario);
				return exito(redirect, "Usuario creado correctamente.");
			}
		} catch (IllegalArgumentException ex) {
			errores.reject("regla", ex.getMessage());
		} finally {
			formulario.limpiarSecretos();
		}
		return prepararFormulario(model, null);
	}

	@GetMapping("/{id}/editar")
	String editar(@PathVariable Long id, Model model) {
		UsuarioVista usuario = usuarios.consultar(id);
		EditarUsuarioForm formulario = new EditarUsuarioForm();
		formulario.setNombreUsuario(usuario.nombreUsuario());
		formulario.setNombreCompleto(usuario.nombreCompleto());
		formulario.setRol(usuario.rol());
		model.addAttribute("formulario", formulario);
		return prepararFormulario(model, id);
	}

	@PostMapping("/{id}")
	String guardarEdicion(@PathVariable Long id, @Valid @ModelAttribute("formulario") EditarUsuarioForm formulario,
		BindingResult errores, Model model, RedirectAttributes redirect) {
		try {
			if (!errores.hasErrors()) {
				usuarios.editar(id, formulario);
				return exito(redirect, "Usuario actualizado correctamente.");
			}
		} catch (IllegalArgumentException ex) {
			errores.reject("regla", ex.getMessage());
		}
		return prepararFormulario(model, id);
	}

	@PostMapping("/{id}/activar")
	String activar(@PathVariable Long id, RedirectAttributes redirect) {
		return cambiarEstado(id, true, redirect);
	}

	@PostMapping("/{id}/desactivar")
	String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
		return cambiarEstado(id, false, redirect);
	}

	@GetMapping("/{id}/restablecer-password")
	String formularioContrasena(@PathVariable Long id, Model model) {
		model.addAttribute("formulario", new RestablecerContrasenaForm());
		return prepararRestablecimiento(id, model);
	}

	@PostMapping("/{id}/restablecer-password")
	String restablecer(@PathVariable Long id, @ModelAttribute("formulario") RestablecerContrasenaForm formulario,
		Model model, RedirectAttributes redirect) {
		try {
			usuarios.restablecer(id, formulario);
			return exito(redirect, "Contraseña restablecida. Las sesiones anteriores han finalizado.");
		} catch (IllegalArgumentException ex) {
			model.addAttribute("error", ex.getMessage());
		} finally {
			formulario.limpiarSecretos();
		}
		return prepararRestablecimiento(id, model);
	}

	private String cambiarEstado(Long id, boolean activo, RedirectAttributes redirect) {
		try {
			usuarios.cambiarEstado(id, activo);
			return exito(redirect, activo ? "Cuenta activada." : "Cuenta desactivada. Sus sesiones han finalizado.");
		} catch (IllegalArgumentException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
			return "redirect:/admin/usuarios";
		}
	}

	private String prepararFormulario(Model model, Long id) {
		model.addAttribute("creacion", id == null);
		model.addAttribute("accion", id == null ? "/admin/usuarios" : "/admin/usuarios/" + id);
		model.addAttribute("roles", Rol.values());
		return "usuarios/formulario";
	}

	private String prepararRestablecimiento(Long id, Model model) {
		model.addAttribute("usuario", usuarios.consultar(id));
		return "usuarios/restablecer";
	}

	private String exito(RedirectAttributes redirect, String mensaje) {
		redirect.addFlashAttribute("mensaje", mensaje);
		return "redirect:/admin/usuarios";
	}
}
