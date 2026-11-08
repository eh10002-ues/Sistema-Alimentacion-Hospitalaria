package sv.edu.ues.alimentacion_hospitalaria.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.inicio.PanelRol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.ServicioContrasenas;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DashboardPermisosTests.RutasPruebaConfig.class})
@Transactional
class DashboardPermisosTests {
	@Autowired private MockMvc mvc;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private ServicioContrasenas contrasenas;
	@Autowired private RutasPrueba rutas;

	@ParameterizedTest
	@EnumSource(Rol.class)
	void loginRealAbreElPanelDeCadaRolSinEnlacesInexistentes(Rol rol) throws Exception {
		String clave = "Aa1" + UUID.randomUUID();
		String nombre = "usuario.demo";
		usuarios.save(new Usuario(nombre, contrasenas.codificar(nombre, clave, clave), "Usuario sintético", rol, true));
		var resultado = mvc.perform(formLogin().user(nombre).password(clave))
			.andExpect(redirectedUrl("/dashboard")).andReturn();
		var sesion = (org.springframework.mock.web.MockHttpSession) resultado.getRequest().getSession(false);
		var panel = mvc.perform(get("/dashboard").session(sesion)).andExpect(status().isOk())
			.andExpect(view().name("index")).andReturn();
		assertThat(((PanelRol) panel.getModelAndView().getModel().get("panel")).rol()).isEqualTo(rol);
		String html = panel.getResponse().getContentAsString();
		assertThat(html).contains(rol.getEtiqueta(), "Funciones de su rol", "Cerrar sesión")
			.doesNotContain(clave, "href=\"/menus", "href=\"/asignaciones", "href=\"/reportes/");
		if (rol == Rol.ROLE_ADMIN) {
			assertThat(html).contains("Administrar usuarios", "href=\"/admin/usuarios\"");
		} else {
			assertThat(html).doesNotContain("href=\"/admin/usuarios\"", "Crear y mantener cuentas de usuario");
		}
		if (rol == Rol.ROLE_COCINA) {
			assertThat(html).doesNotContain("href=\"/pacientes\"");
		}
		mvc.perform(get("/").session(sesion)).andExpect(redirectedUrl("/dashboard"));
	}

	@ParameterizedTest(name = "{0}: {1} {2} permitido={3}")
	@MethodSource("matriz")
	void servidorAplicaLaMatrizSinEjecutarOperacionesDenegadas(Rol rol, String metodo, String ruta, boolean permitido) throws Exception {
		// El sufijo evita ejecutar los controladores funcionales incorporados al proyecto.
		if (!ruta.equals("/admin/prueba") && !ruta.equals("/sin-regla")) {
			ruta = ruta + (ruta.endsWith("/") ? "" : "/") + "prueba-permisos";
		}
		int antes = rutas.invocaciones.get();
		var resultado = mvc.perform(request(HttpMethod.valueOf(metodo), ruta)
			.with(user("prueba").authorities(new SimpleGrantedAuthority(rol.name()))).with(csrf()));
		if (permitido) {
			resultado.andExpect(status().isNoContent());
			assertThat(rutas.invocaciones.get()).isEqualTo(antes + 1);
		} else {
			resultado.andExpect(status().isForbidden()).andExpect(forwardedUrl("/acceso-denegado"));
			assertThat(rutas.invocaciones.get()).isEqualTo(antes);
		}
	}

	static Stream<Arguments> matriz() {
		Set<Rol> todos = Set.of(Rol.values());
		Set<Rol> clinicosPiso = Set.of(Rol.ROLE_ADMIN, Rol.ROLE_MEDICO, Rol.ROLE_NUTRICIONISTA, Rol.ROLE_ENCARGADO_PISO);
		Set<Rol> nutricion = Set.of(Rol.ROLE_ADMIN, Rol.ROLE_NUTRICIONISTA);
		Set<Rol> medico = Set.of(Rol.ROLE_ADMIN, Rol.ROLE_MEDICO);
		Set<Rol> piso = Set.of(Rol.ROLE_ADMIN, Rol.ROLE_ENCARGADO_PISO);
		Set<Rol> asignaciones = Set.of(Rol.ROLE_ADMIN, Rol.ROLE_MEDICO, Rol.ROLE_NUTRICIONISTA);
		List<Caso> casos = List.of(
			new Caso("/pacientes/1", clinicosPiso, medico),
			new Caso("/ubicaciones/1", piso, piso),
			new Caso("/menus/1", todos, nutricion),
			new Caso("/dietas/1", todos, nutricion),
			new Caso("/asignaciones/1", asignaciones, asignaciones),
			new Caso("/reportes/pacientes", clinicosPiso, Set.of()),
			new Caso("/reportes/ocupacion", clinicosPiso, Set.of()),
			new Caso("/reportes/asignaciones", todos, Set.of()),
			new Caso("/reportes/menus", todos, Set.of()));
		Stream<Arguments> generales = Arrays.stream(Rol.values()).flatMap(rol -> casos.stream().flatMap(caso ->
			Stream.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE").map(metodo -> {
				boolean lectura = metodo.equals("GET") || metodo.equals("HEAD");
				boolean eliminadoNoDefinido = metodo.equals("DELETE") && (caso.ruta.startsWith("/pacientes") || caso.ruta.startsWith("/asignaciones"));
				return Arguments.of(rol, metodo, caso.ruta, !eliminadoNoDefinido && (lectura ? caso.lectura : caso.escritura).contains(rol));
			})));
		Stream<Arguments> especificos = Arrays.stream(Rol.values()).flatMap(rol -> Stream.of(
			Arguments.of(rol, "GET", "/menus/nuevo", nutricion.contains(rol)),
			Arguments.of(rol, "GET", "/menus/nuevo/", nutricion.contains(rol)),
			Arguments.of(rol, "GET", "/menus/1/editar", nutricion.contains(rol)),
			Arguments.of(rol, "GET", "/dietas/nuevo", nutricion.contains(rol)),
			Arguments.of(rol, "GET", "/pacientes/1/editar", medico.contains(rol)),
			Arguments.of(rol, "GET", "/ubicaciones/nuevo", piso.contains(rol)),
			Arguments.of(rol, "POST", "/asignaciones/1/aprobar", nutricion.contains(rol)),
			Arguments.of(rol, "POST", "/asignaciones/1/rechazar", nutricion.contains(rol)),
			Arguments.of(rol, "POST", "/asignaciones/1/aprobar/", nutricion.contains(rol)),
			Arguments.of(rol, "PUT", "/asignaciones/1/aprobar", false),
			Arguments.of(rol, "GET", "/reportes/pacientes/impresion", clinicosPiso.contains(rol)),
			Arguments.of(rol, "GET", "/admin/prueba", rol == Rol.ROLE_ADMIN),
			Arguments.of(rol, "POST", "/sin-regla", false)));
		return Stream.concat(generales, especificos);
	}

	@Test
	void dashboardExigeAutenticacionYRechazaRolesNoDefinidos() throws Exception {
		mvc.perform(get("/dashboard")).andExpect(redirectedUrl("/login"));
		mvc.perform(get("/dashboard").with(user("prueba").roles("OTRO"))).andExpect(status().isForbidden());
		mvc.perform(get("/dashboard").with(user("prueba").roles("MEDICO", "NUTRICIONISTA"))).andExpect(status().isForbidden());
	}

	@Test
	void error403EsComprensibleYPermiteRegresarAlPanel() throws Exception {
		mvc.perform(get("/acceso-denegado").with(user("prueba").roles("COCINA")))
			.andExpect(status().isForbidden()).andExpect(view().name("error/403"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Volver al panel principal")));
	}

	@Test
	void permisosDeModificacionNoDesactivanCsrf() throws Exception {
		mvc.perform(post("/menus/1").with(user("prueba").roles("NUTRICIONISTA")))
			.andExpect(status().isForbidden());
	}

	private record Caso(String ruta, Set<Rol> lectura, Set<Rol> escritura) { }

	@TestConfiguration(proxyBeanMethods = false)
	static class RutasPruebaConfig {
		@Bean RutasPrueba rutasPrueba() { return new RutasPrueba(); }
	}

	// Controlador exclusivo de pruebas: permite comprobar la cadena sin implementar módulos pendientes.
	@RestController
	@TestComponent
	static class RutasPrueba {
		final AtomicInteger invocaciones = new AtomicInteger();
		@RequestMapping({"/pacientes/**", "/ubicaciones/**", "/menus/**", "/dietas/**", "/asignaciones/**", "/reportes/**", "/admin/prueba", "/sin-regla"})
		ResponseEntity<Void> operacion() {
			invocaciones.incrementAndGet();
			return ResponseEntity.noContent().build();
		}
	}
}
