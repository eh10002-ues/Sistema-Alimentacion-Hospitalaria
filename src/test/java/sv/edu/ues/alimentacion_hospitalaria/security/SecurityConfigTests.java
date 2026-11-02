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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;

@SpringBootTest(properties = "spring.security.user.password=" + SecurityConfigTests.PASSWORD)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SecurityConfigTests {

	static final String PASSWORD = "test-password";

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
	@WithMockUser
	void authenticatedUserCanSeeHome() throws Exception {
		mvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("index"));
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
		mvc.perform(formLogin("/login").user("user").password(PASSWORD))
			.andExpect(redirectedUrl("/"))
			.andExpect(authenticated().withUsername("user"));
	}

	@Test
	void loginWithInvalidCredentialsIsRejected() throws Exception {
		mvc.perform(formLogin("/login").user("user").password("incorrecta"))
			.andExpect(redirectedUrl("/login?error"))
			.andExpect(unauthenticated());
	}

	@Test
	void loginWithoutValidCsrfTokenIsRejected() throws Exception {
		mvc.perform(post("/login")
				.param("username", "user")
				.param("password", PASSWORD))
			.andExpect(status().isForbidden())
			.andExpect(unauthenticated());

		mvc.perform(post("/login")
				.param("username", "user")
				.param("password", PASSWORD)
				.with(csrf().useInvalidToken()))
			.andExpect(status().isForbidden())
			.andExpect(unauthenticated());
	}
}
