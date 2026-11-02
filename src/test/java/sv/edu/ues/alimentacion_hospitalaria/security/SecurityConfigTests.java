package sv.edu.ues.alimentacion_hospitalaria.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.ServicioContrasenas;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class SecurityConfigTests {

	private String password;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private ServicioContrasenas contrasenas;

	@BeforeEach
	void crearUsuarioSintetico() {
		password = "Aa1" + UUID.randomUUID();
		usuarios.save(new Usuario("medico.demo", contrasenas.codificar("medico.demo", password, password),
			"Médico de demostración", Rol.ROLE_MEDICO, true));
	}

	@Autowired
	private MockMvc mvc;

	@Test
	void anonymousUserIsRedirectedToLogin() throws Exception {
		mvc.perform(get("/"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}

	@Test
	void loginPageIsPublic() throws Exception {
		mvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("login"));
	}

	@Test
	void staticResourcesArePublic() throws Exception {
		mvc.perform(get("/css/styles.css"))
			.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "MEDICO")
	void authenticatedUserCanSeeHome() throws Exception {
		mvc.perform(get("/"))
			.andExpect(redirectedUrl("/dashboard"));
	}

	@Test
	@WithMockUser
	void logoutRequiresCsrfToken() throws Exception {
		mvc.perform(post("/logout"))
			.andExpect(status().isForbidden());

		mvc.perform(post("/logout").with(csrf()))
			.andExpect(redirectedUrl("/login?logout"));
	}

	@Test
	void loginWithValidCredentialsRedirectsToHome() throws Exception {
		mvc.perform(formLogin("/login").user("MEDICO.DEMO").password(password))
			.andExpect(redirectedUrl("/dashboard"))
			.andExpect(authenticated().withUsername("medico.demo"));
	}

	@Test
	void loginWithInvalidCredentialsIsRejected() throws Exception {
		mvc.perform(formLogin("/login").user("medico.demo").password(UUID.randomUUID().toString()))
			.andExpect(redirectedUrl("/login?error"))
			.andExpect(unauthenticated());
	}

	@Test
	void loginWithoutValidCsrfTokenIsRejected() throws Exception {
		mvc.perform(post("/login")
				.param("username", "medico.demo")
				.param("password", password))
			.andExpect(status().isForbidden())
			.andExpect(unauthenticated());

		mvc.perform(post("/login")
				.param("username", "medico.demo")
				.param("password", password)
				.with(csrf().useInvalidToken()))
			.andExpect(status().isForbidden())
			.andExpect(unauthenticated());
	}

	@ParameterizedTest
	@EnumSource(Rol.class)
	void cargaLaAutoridadExactaDeCadaRol(Rol rol) throws Exception {
		var usuario = usuarios.findByNombreUsuarioIgnoreCase("medico.demo").orElseThrow();
		usuario.setRol(rol);
		usuarios.save(usuario);
		mvc.perform(formLogin().user("medico.demo").password(password))
			.andExpect(authenticated().withAuthentication(authentication ->
				org.assertj.core.api.Assertions.assertThat(authentication.getAuthorities())
					.extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
					.filteredOn(authority -> authority.startsWith("ROLE_"))
					.containsExactly(rol.name())));
	}

	@Test
	void usuarioInactivoNoPuedeAutenticarse() throws Exception {
		var usuario = usuarios.findByNombreUsuarioIgnoreCase("medico.demo").orElseThrow();
		usuario.setActivo(false);
		usuarios.save(usuario);
		mvc.perform(formLogin().user("medico.demo").password(password))
			.andExpect(redirectedUrl("/login?error"))
			.andExpect(unauthenticated());
	}

	@ParameterizedTest
	@ValueSource(strings = {"inexistente", "user", "", " medico.demo", "medico.demo "})
	void nombresNoRegistradosOVaciosNoPuedenAutenticarse(String nombre) throws Exception {
		mvc.perform(formLogin().user(nombre).password(password))
			.andExpect(redirectedUrl("/login?error"))
			.andExpect(unauthenticated());
	}

	@Test
	void contrasenaDistingueMayusculas() throws Exception {
		mvc.perform(formLogin().user("medico.demo").password(password.toLowerCase(java.util.Locale.ROOT)))
			.andExpect(redirectedUrl("/login?error"))
			.andExpect(unauthenticated());
	}

	@Test
	void errorDeLoginEsGenericoYNoExponeSecretos() throws Exception {
		String html = mvc.perform(get("/login?error")).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(html)
			.contains("Usuario o contraseña incorrectos.")
			.doesNotContain(password, usuarios.findByNombreUsuarioIgnoreCase("medico.demo").orElseThrow().getContrasenaHash());
	}

}
