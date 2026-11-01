package sv.edu.ues.alimentacion_hospitalaria.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

	@Bean
	AuditorAware<String> auditorAware() {
		return () -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
				// procesos sin usuario autenticado
				return Optional.of("system");
			}
			return Optional.of(auth.getName());
		};
	}
}
