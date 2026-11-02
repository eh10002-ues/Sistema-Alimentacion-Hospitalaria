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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new DelegatingPasswordEncoder("pbkdf2@SpringSecurity_v5_8", Map.of(
			"pbkdf2@SpringSecurity_v5_8", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
				.anyRequest().authenticated())
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
				.defaultSuccessUrl("/", true)
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/login?logout")
				.permitAll());
		return http.build();
	}
}
