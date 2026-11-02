package sv.edu.ues.alimentacion_hospitalaria.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@EnabledIfEnvironmentVariable(named = "HU01_PLAYWRIGHT_MODULE", matches = ".+")
class LogoutBrowserTests {
	@LocalServerPort private int puerto;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private ServicioContrasenas contrasenas;

	@Test
	void navegadorNoRecuperaElPanelAlVolverAtrasDespuesDelLogout() throws Exception {
		StringBuilder entrada = new StringBuilder("http://127.0.0.1:" + puerto + "\n");
		for (Rol rol : Rol.values()) {
			String nombre = "browser." + UUID.randomUUID();
			String clave = "Aa1" + UUID.randomUUID();
			usuarios.save(new Usuario(nombre, contrasenas.codificar(nombre, clave, clave), "Usuario sintético", rol, true));
			entrada.append(rol.name()).append('\t').append(nombre).append('\t').append(clave)
				.append('\t').append(rol.getEtiqueta()).append('\n');
		}
		ProcessBuilder builder = new ProcessBuilder("node", Path.of("src/test/resources/hu01-logout-browser.cjs").toString(),
			System.getenv("HU01_PLAYWRIGHT_MODULE")).redirectErrorStream(true);
		builder.environment().remove("DEBUG");
		builder.environment().remove("PWDEBUG");
		Process proceso = builder.start();
		try {
			try (var stdin = proceso.getOutputStream()) {
				// Las claves se transmiten por stdin; nunca como argumentos, archivos o registros.
				stdin.write(entrada.toString().getBytes(StandardCharsets.UTF_8));
			}
			assertThat(proceso.waitFor(60, TimeUnit.SECONDS)).as("Chrome debe completar la prueba sin capturas").isTrue();
			String resumen = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			assertThat(proceso.exitValue()).as(resumen).isZero();
		} finally {
			if (proceso.isAlive()) proceso.destroyForcibly();
		}
	}
}
