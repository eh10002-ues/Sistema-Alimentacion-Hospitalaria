package sv.edu.ues.alimentacion_hospitalaria.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.PrimerAdministradorService;

class InicializacionAdministradorConfigTests {

	@Test
	void perfilExplicitoRegistraRunnerQueLeeLasTresVariables() {
		String clave = "Aa1" + UUID.randomUUID();
		var servicio = mock(PrimerAdministradorService.class);
		new ApplicationContextRunner()
			.withUserConfiguration(InicializacionAdministradorConfig.class)
			.withBean(PrimerAdministradorService.class, () -> servicio)
			.withPropertyValues("spring.profiles.active=inicializacion", "APP_ADMIN_USERNAME=ADMIN.DEMO",
				"APP_ADMIN_PASSWORD=" + clave, "APP_ADMIN_FULL_NAME=Administrador de prueba")
			.run(contexto -> {
				assertThat(contexto).hasSingleBean(ApplicationRunner.class);
				contexto.getBean(ApplicationRunner.class).run(new DefaultApplicationArguments());
				verify(servicio).crearSiNoExiste("ADMIN.DEMO", clave, "Administrador de prueba");
			});
	}
}
