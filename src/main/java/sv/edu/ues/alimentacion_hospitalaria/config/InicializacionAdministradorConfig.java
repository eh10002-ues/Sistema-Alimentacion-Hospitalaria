package sv.edu.ues.alimentacion_hospitalaria.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.PrimerAdministradorService;

@Configuration(proxyBeanMethods = false)
@Profile("inicializacion")
public class InicializacionAdministradorConfig {

	@Bean
	ApplicationRunner primerAdministrador(Environment environment, PrimerAdministradorService servicio) {
		return args -> servicio.crearSiNoExiste(
			environment.getProperty("APP_ADMIN_USERNAME"),
			environment.getProperty("APP_ADMIN_PASSWORD"),
			environment.getProperty("APP_ADMIN_FULL_NAME"));
	}
}
