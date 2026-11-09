package sv.edu.ues.alimentacion_hospitalaria.usuarios;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.*;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.dto.*;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.AdministracionUsuariosService;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AdministracionUsuariosTests {
	@Autowired private MockMvc mvc;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private PasswordEncoder encoder;
	@Autowired private SessionRegistry sesiones;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private AdministracionUsuariosService servicio;
	@Autowired private PlatformTransactionManager transacciones;
	private String clave;
	private Long adminId;
	private Long medicoId;

	@BeforeEach
	void prepararBaseDescartable() {
		SecurityContextHolder.clearContext();
		for (Object principal : sesiones.getAllPrincipals()) {
			sesiones.getAllSessions(principal, true).forEach(s -> sesiones.removeSessionInformation(s.getSessionId()));
		}
		// Exclusivamente la base test de Testcontainers; cada caso usa operaciones confirmadas.
		jdbc.update("DELETE FROM usuarios");
		clave = "Aa1" + UUID.randomUUID();
		String hash = encoder.encode(clave);
		adminId = usuarios.save(new Usuario("admin.demo", hash, "Administrador de prueba", Rol.ROLE_ADMIN, true)).getId();
		medicoId = usuarios.save(new Usuario("medico.demo", hash, "Médico de prueba", Rol.ROLE_MEDICO, true)).getId();
	}

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
		jdbc.update("DELETE FROM usuarios");
	}

	@Test
	void administradorPuedeVerTodasLasVistasSinHashes() throws Exception {
		MockHttpSession sesion = login("admin.demo");
		String hash = usuarios.findById(medicoId).orElseThrow().getContrasenaHash();
		for (String ruta : List.of("/admin/usuarios", "/admin/usuarios/nuevo", "/admin/usuarios/" + medicoId + "/editar",
			"/admin/usuarios/" + medicoId + "/restablecer-password")) {
			String html = mvc.perform(get(ruta).session(sesion)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
			assertThat(html).doesNotContain(hash, clave).contains("Cerrar sesión");
		}
	}

	@ParameterizedTest
	@EnumSource(value = Rol.class, names = "ROLE_ADMIN", mode = EnumSource.Mode.EXCLUDE)
	void otrosRolesNoPuedenAdministrarNiInvocarElServicio(Rol rol) throws Exception {
		Usuario usuario = usuarios.findById(medicoId).orElseThrow();
		usuario.setRol(rol);
		usuarios.save(usuario);
		MockHttpSession sesion = login("medico.demo");
		mvc.perform(get("/admin/usuarios").session(sesion)).andExpect(status().isForbidden());
		mvc.perform(post("/admin/usuarios/" + adminId + "/desactivar").session(sesion).with(csrf()))
			.andExpect(status().isForbidden());
		mvc.perform(post("/admin/usuarios").session(sesion).with(csrf()))
			.andExpect(status().isForbidden());
		como("medico.demo", rol);
		assertThatThrownBy(() -> servicio.listar(0)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}

	@Test
	void crearRequiereCsrfYGuardaElHashSinDevolverLaClave() throws Exception {
		MockHttpSession sesion = login("admin.demo");
		mvc.perform(post("/admin/usuarios").session(sesion)).andExpect(status().isForbidden());
		mvc.perform(post("/admin/usuarios").session(sesion).with(csrf())
			.param("nombreUsuario", "COCINA.DEMO").param("nombreCompleto", "Cocina de prueba")
			.param("rol", "ROLE_COCINA").param("activo", "true").param("contrasena", clave).param("confirmacion", clave))
			.andExpect(redirectedUrl("/admin/usuarios"));
		Usuario creado = usuarios.findByNombreUsuarioIgnoreCase("cocina.demo").orElseThrow();
		assertThat(creado.getContrasenaHash()).isNotEqualTo(clave);
		assertThat(encoder.matches(clave, creado.getContrasenaHash())).isTrue();
		assertThat(creado.getRol()).isEqualTo(Rol.ROLE_COCINA);
	}

	@ParameterizedTest
	@ValueSource(strings = {"duplicado", "rol", "confirmacion", "nombre"})
	void erroresDeCreacionConservanLaBaseYNoDevuelvenSecretos(String caso) throws Exception {
		MockHttpSession sesion = login("admin.demo");
		var resultado = mvc.perform(post("/admin/usuarios").session(sesion).with(csrf())
			.param("nombreUsuario", caso.equals("duplicado") ? "MEDICO.DEMO" : "nuevo.demo")
			.param("nombreCompleto", caso.equals("nombre") ? "" : "Nombre de prueba")
			.param("rol", caso.equals("rol") ? "ROLE_OTRO" : "ROLE_COCINA")
			.param("contrasena", clave).param("confirmacion", caso.equals("confirmacion") ? "" : clave))
			.andExpect(status().isOk()).andExpect(view().name("usuarios/formulario")).andReturn();
		assertThat(resultado.getResponse().getContentAsString()).doesNotContain(clave);
		CrearUsuarioForm dto = (CrearUsuarioForm) resultado.getModelAndView().getModel().get("formulario");
		assertThat(dto.getContrasena()).isNull();
		assertThat(dto.getConfirmacion()).isNull();
		assertThat(usuarios.findAll(org.springframework.data.domain.PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
	}

	@Test
	void edicionNoPermiteCambiarHashOEstadoPorAsignacionMasiva() throws Exception {
		String hash = usuarios.findById(medicoId).orElseThrow().getContrasenaHash();
		mvc.perform(post("/admin/usuarios/" + medicoId).session(login("admin.demo")).with(csrf())
			.param("nombreUsuario", "medico.demo").param("nombreCompleto", "Nombre actualizado").param("rol", "ROLE_MEDICO")
			.param("activo", "false").param("contrasenaHash", "valor no permitido"))
			.andExpect(redirectedUrl("/admin/usuarios"));
		Usuario actualizado = usuarios.findById(medicoId).orElseThrow();
		assertThat(actualizado.isActivo()).isTrue();
		assertThat(actualizado.getContrasenaHash()).isEqualTo(hash);
		assertThat(actualizado.getNombreCompleto()).isEqualTo("Nombre actualizado");
	}

	@Test
	void noPermiteDuplicarNombreEnEdicionNiDesactivarLaCuentaPropia() throws Exception {
		MockHttpSession sesion = login("admin.demo");
		mvc.perform(post("/admin/usuarios/" + medicoId).session(sesion).with(csrf())
			.param("nombreUsuario", "ADMIN.DEMO").param("nombreCompleto", "Nombre").param("rol", "ROLE_MEDICO"))
			.andExpect(status().isOk()).andExpect(view().name("usuarios/formulario"));
		mvc.perform(post("/admin/usuarios/" + adminId + "/desactivar").session(sesion).with(csrf()))
			.andExpect(flash().attribute("error", "No puede desactivar su propia cuenta."));
		assertThat(usuarios.findById(adminId).orElseThrow().isActivo()).isTrue();
		assertThat(usuarios.findById(medicoId).orElseThrow().getNombreUsuario()).isEqualTo("medico.demo");
	}

	@Test
	void protegeElRolDelUltimoAdministrador() throws Exception {
		mvc.perform(post("/admin/usuarios/" + adminId).session(login("admin.demo")).with(csrf())
			.param("nombreUsuario", "admin.demo").param("nombreCompleto", "Administrador").param("rol", "ROLE_MEDICO"))
			.andExpect(status().isOk()).andExpect(view().name("usuarios/formulario"));
		assertThat(usuarios.findById(adminId).orElseThrow().getRol()).isEqualTo(Rol.ROLE_ADMIN);
	}

	@ParameterizedTest
	@ValueSource(strings = {"desactivar", "rol", "password"})
	void modificacionesRevocanTodasLasSesionesYReactivacionNoLasRestaura(String operacion) throws Exception {
		MockHttpSession primera = login("medico.demo");
		MockHttpSession segunda = login("medico.demo");
		MockHttpSession admin = login("admin.demo");
		String nueva = "Aa1" + UUID.randomUUID();
		if (operacion.equals("desactivar")) {
			mvc.perform(post("/admin/usuarios/" + medicoId + "/desactivar").session(admin).with(csrf()))
				.andExpect(redirectedUrl("/admin/usuarios"));
		} else if (operacion.equals("rol")) {
			mvc.perform(post("/admin/usuarios/" + medicoId).session(admin).with(csrf())
				.param("nombreUsuario", "medico.demo").param("nombreCompleto", "Médico").param("rol", "ROLE_COCINA"))
				.andExpect(redirectedUrl("/admin/usuarios"));
		} else {
			mvc.perform(post("/admin/usuarios/" + medicoId + "/restablecer-password").session(admin).with(csrf())
				.param("contrasena", nueva).param("confirmacion", nueva)).andExpect(redirectedUrl("/admin/usuarios"));
		}
		assertThat(sesiones.getSessionInformation(primera.getId()).isExpired()).isTrue();
		assertThat(sesiones.getSessionInformation(segunda.getId()).isExpired()).isTrue();
		if (operacion.equals("desactivar")) {
			mvc.perform(formLogin().user("medico.demo").password(clave)).andExpect(redirectedUrl("/login?error"));
			mvc.perform(post("/admin/usuarios/" + medicoId + "/activar").session(admin).with(csrf()))
				.andExpect(redirectedUrl("/admin/usuarios"));
		}
		for (MockHttpSession antigua : List.of(primera, segunda)) {
			mvc.perform(get("/").session(antigua)).andExpect(redirectedUrl("/login?expired"));
		}
		String valida = operacion.equals("password") ? nueva : clave;
		mvc.perform(formLogin().user("medico.demo").password(valida))
			.andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated()
				.withAuthentication(auth -> assertThat(auth.getAuthorities())
					.extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
					.contains(operacion.equals("rol") ? "ROLE_COCINA" : "ROLE_MEDICO")));
		if (operacion.equals("password")) {
			mvc.perform(formLogin().user("medico.demo").password(clave)).andExpect(redirectedUrl("/login?error"));
		}
		mvc.perform(get("/admin/usuarios").session(admin)).andExpect(status().isOk());
	}

	@Test
	void renombrarUsuarioRevocaSuIdentidadAnterior() throws Exception {
		MockHttpSession anterior = login("medico.demo");
		mvc.perform(post("/admin/usuarios/" + medicoId).session(login("admin.demo")).with(csrf())
			.param("nombreUsuario", "MEDICO.NUEVO").param("nombreCompleto", "Médico").param("rol", "ROLE_MEDICO"))
			.andExpect(redirectedUrl("/admin/usuarios"));
		mvc.perform(post("/admin/usuarios/" + adminId + "/desactivar").session(anterior).with(csrf()))
			.andExpect(redirectedUrl("/login?expired"));
		assertThat(usuarios.findById(adminId).orElseThrow().isActivo()).isTrue();
		mvc.perform(formLogin().user("medico.demo").password(clave)).andExpect(redirectedUrl("/login?error"));
		login("medico.nuevo");
	}

	@Test
	void restablecimientoInvalidoNoCambiaHashNiRevocaSesiones() throws Exception {
		MockHttpSession medico = login("medico.demo");
		String hash = usuarios.findById(medicoId).orElseThrow().getContrasenaHash();
		String html = mvc.perform(post("/admin/usuarios/" + medicoId + "/restablecer-password")
			.session(login("admin.demo")).with(csrf()).param("contrasena", clave).param("confirmacion", ""))
			.andExpect(view().name("usuarios/restablecer")).andReturn().getResponse().getContentAsString();
		assertThat(html).doesNotContain(clave, hash);
		assertThat(usuarios.findById(medicoId).orElseThrow().getContrasenaHash()).isEqualTo(hash);
		assertThat(sesiones.getSessionInformation(medico.getId()).isExpired()).isFalse();
	}

	@Test
	void rollbackNoRevocaSesionesNiAplicaCambios() throws Exception {
		MockHttpSession medico = login("medico.demo");
		como("admin.demo", Rol.ROLE_ADMIN);
		assertThatThrownBy(() -> new TransactionTemplate(transacciones).execute(status -> {
			servicio.editar(medicoId, edicion("medico.demo", Rol.ROLE_COCINA));
			throw new IllegalStateException("Fallo controlado de prueba");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(sesiones.getSessionInformation(medico.getId()).isExpired()).isFalse();
		assertThat(usuarios.findById(medicoId).orElseThrow().getRol()).isEqualTo(Rol.ROLE_MEDICO);
	}

	@Test
	void cambiosSimultaneosConservanUnAdministradorActivo() throws Exception {
		Long otroId = usuarios.save(new Usuario("admin.otro", encoder.encode(clave), "Otro administrador", Rol.ROLE_ADMIN, true)).getId();
		CountDownLatch inicio = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Callable<Boolean> primero = () -> intentarCambio(inicio, adminId, "admin.demo");
			Callable<Boolean> segundo = () -> intentarCambio(inicio, otroId, "admin.otro");
			Future<Boolean> a = executor.submit(primero);
			Future<Boolean> b = executor.submit(segundo);
			inicio.countDown();
			assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
				.containsExactlyInAnyOrder(true, false);
		}
		assertThat(usuarios.countByRolAndActivoTrue(Rol.ROLE_ADMIN)).isEqualTo(1);
	}

	@Test
	void usuarioInexistenteDevuelve404YListadoEsPaginado() throws Exception {
		MockHttpSession sesion = login("admin.demo");
		mvc.perform(get("/admin/usuarios/999999/editar").session(sesion)).andExpect(status().isNotFound());
		como("admin.demo", Rol.ROLE_ADMIN);
		String hash = usuarios.findById(adminId).orElseThrow().getContrasenaHash();
		for (int i = 0; i < 21; i++) {
			usuarios.save(new Usuario("usuario." + i, hash, "Usuario sintético", Rol.ROLE_COCINA, false));
		}
		assertThat(servicio.listar(0).getNumberOfElements()).isEqualTo(20);
		assertThat(servicio.listar(1).getNumberOfElements()).isEqualTo(3);
		assertThat(servicio.listar(99)).isEmpty();
	}

	private boolean intentarCambio(CountDownLatch inicio, Long id, String nombre) throws Exception {
		inicio.await(10, TimeUnit.SECONDS);
		como(nombre, Rol.ROLE_ADMIN);
		try {
			servicio.editar(id, edicion(nombre, Rol.ROLE_MEDICO));
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private EditarUsuarioForm edicion(String nombre, Rol rol) {
		EditarUsuarioForm formulario = new EditarUsuarioForm();
		formulario.setNombreUsuario(nombre);
		formulario.setNombreCompleto("Nombre de prueba");
		formulario.setRol(rol);
		return formulario;
	}

	private void como(String nombre, Rol rol) {
		SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
			nombre, null, List.of(new SimpleGrantedAuthority(rol.name()))));
	}

	private MockHttpSession login(String nombre) throws Exception {
		return (MockHttpSession) mvc.perform(formLogin().user(nombre).password(clave))
			.andExpect(redirectedUrl("/dashboard")).andReturn().getRequest().getSession(false);
	}
}
