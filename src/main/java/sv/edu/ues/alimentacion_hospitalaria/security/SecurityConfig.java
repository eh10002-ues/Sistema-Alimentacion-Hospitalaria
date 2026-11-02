package sv.edu.ues.alimentacion_hospitalaria.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// Mientras no existan usuarios en BD, Boot crea el usuario "user" con contraseña generada (ver log).
// El PasswordEncoder se agrega junto con el UserDetailsService para no romper ese usuario temporal.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
				.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/", true)
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/login?logout")
				.permitAll());
		return http.build();
	}
}
