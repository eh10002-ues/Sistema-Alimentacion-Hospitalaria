package sv.edu.ues.alimentacion_hospitalaria.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.ServicioContrasenas;

// Servidor HTTP real: comprueba cookies y reenvíos que MockMvc no ejecuta.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class LogoutHttpTests {
	@LocalServerPort private int puerto;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private ServicioContrasenas contrasenas;
	@Autowired private org.springframework.security.core.session.SessionRegistry sesiones;

	@ParameterizedTest
	@EnumSource(Rol.class)
	void logoutRechazaCsrfInvalidoYNoPermiteReutilizarLaSesion(Rol rol) throws Exception {
		String nombre = "logout." + UUID.randomUUID();
		String clave = "Aa1" + UUID.randomUUID();
		usuarios.save(new Usuario(nombre, contrasenas.codificar(nombre, clave, clave), "Usuario sintético", rol, true));
		CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		try (HttpClient cliente = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build()) {
			iniciarSesion(cliente, nombre, clave);
			String anterior = identificador(cookies);
			HttpResponse<String> panel = get(cliente, "/dashboard");
			assertThat(panel.statusCode()).isEqualTo(200);
			assertThat(panel.body().contains(rol.getEtiqueta())).isTrue();
			assertThat(panel.headers().firstValue("Cache-Control").orElse("")).contains("no-store", "no-cache");
			if (rol != Rol.ROLE_ADMIN) {
				assertThat(post(cliente, "/admin/usuarios", Map.of("_csrf", csrf(panel))).statusCode()).isEqualTo(403);
			}

			assertThat(post(cliente, "/logout", Map.of()).statusCode()).isEqualTo(403);
			assertThat(post(cliente, "/logout", Map.of("_csrf", "invalido")).statusCode()).isEqualTo(403);
			panel = get(cliente, "/dashboard");
			assertThat(panel.statusCode()).isEqualTo(200);
			HttpResponse<String> salida = post(cliente, "/logout", Map.of("_csrf", csrf(panel)));
			assertThat(salida.statusCode()).isEqualTo(302);
			assertThat(salida.headers().firstValue("Location").orElse("")).endsWith("/login?logout");
			assertThat(salida.headers().allValues("Set-Cookie").stream()
				.flatMap(valor -> java.net.HttpCookie.parse(valor).stream())
				.anyMatch(cookie -> cookie.getName().equals("JSESSIONID") && cookie.getMaxAge() == 0))
				.as("La respuesta debe expirar la cookie JSESSIONID").isTrue();
			assertThat(sesiones.getSessionInformation(anterior) == null).isTrue();
			assertThat(get(cliente, "/dashboard").statusCode()).isEqualTo(302);

			try (HttpClient repeticion = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
				HttpRequest request = HttpRequest.newBuilder(uri("/dashboard"))
					.header("Cookie", "JSESSIONID=" + anterior).GET().build();
				HttpResponse<String> respuesta = repeticion.send(request, HttpResponse.BodyHandlers.ofString());
				assertThat(respuesta.statusCode()).isEqualTo(302);
				assertThat(respuesta.headers().firstValue("Location").orElse("")).endsWith("/login");
			}
			iniciarSesion(cliente, nombre, clave);
			assertThat(identificador(cookies).equals(anterior)).isFalse();
		}
	}

	@Test
	void loginSinCsrfDevuelve403TambienSinAutenticacion() throws Exception {
		CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		try (HttpClient cliente = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build()) {
			get(cliente, "/login");
			assertThat(post(cliente, "/login", Map.of("username", "inexistente", "password", UUID.randomUUID().toString()))
				.statusCode()).isEqualTo(403);
		}
	}

	private void iniciarSesion(HttpClient cliente, String nombre, String clave) throws Exception {
		HttpResponse<String> login = get(cliente, "/login");
		HttpResponse<String> respuesta = post(cliente, "/login", Map.of("username", nombre, "password", clave, "_csrf", csrf(login)));
		assertThat(respuesta.statusCode()).isEqualTo(302);
		assertThat(respuesta.headers().firstValue("Location").orElse("")).endsWith("/dashboard");
	}

	private String identificador(CookieManager cookies) {
		return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID"))
			.findFirst().orElseThrow().getValue();
	}

	private String csrf(HttpResponse<String> respuesta) {
		var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(respuesta.body());
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private URI uri(String ruta) {
		return URI.create("http://127.0.0.1:" + puerto + ruta);
	}

	private HttpResponse<String> get(HttpClient cliente, String ruta) throws Exception {
		return cliente.send(HttpRequest.newBuilder(uri(ruta)).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(HttpClient cliente, String ruta, Map<String, String> datos) throws Exception {
		String cuerpo = datos.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
			+ "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
		return cliente.send(HttpRequest.newBuilder(uri(ruta)).header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
	}
}
