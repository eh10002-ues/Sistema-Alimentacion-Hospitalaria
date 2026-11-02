package sv.edu.ues.alimentacion_hospitalaria.security;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new DelegatingPasswordEncoder("pbkdf2@SpringSecurity_v5_8", Map.of(
			"pbkdf2@SpringSecurity_v5_8", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
	}

	@Bean
	SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sesiones, ReglasAcceso reglas) throws Exception {
		reglas.configurar(http);
		http
			.exceptionHandling(errors -> errors.accessDeniedPage("/acceso-denegado"))
			.sessionManagement(session -> session.maximumSessions(-1)
				.sessionRegistry(sesiones).expiredUrl("/login?expired"))
			.formLogin(form -> form
				.loginPage("/login")
				.authenticationDetailsSource(request -> {
					// El filtro de login recorta el identificador antes de cargar al usuario.
					String nombre = request.getParameter("username");
					if (nombre != null && nombre.codePoints().anyMatch(
						c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
						throw new BadCredentialsException("Usuario o contraseña incorrectos.");
					}
					return new WebAuthenticationDetails(request);
				})
				.defaultSuccessUrl("/dashboard", true)
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/login?logout")
				.permitAll());
		return http.build();
	}
}
