package sv.edu.ues.alimentacion_hospitalaria.security;

import java.util.Arrays;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class ReglasAcceso {
	private static final String[] TODOS = {"ADMIN", "MEDICO", "NUTRICIONISTA", "ENCARGADO_PISO", "COCINA"};
	private static final String[] CLINICOS_Y_PISO = {"ADMIN", "MEDICO", "NUTRICIONISTA", "ENCARGADO_PISO"};
	private static final String[] ASIGNACIONES = {"ADMIN", "MEDICO", "NUTRICIONISTA"};
	private static final String[] NUTRICION = {"ADMIN", "NUTRICIONISTA"};
	private static final String[] APROBACIONES = {"/asignaciones/*/aprobar", "/asignaciones/*/aprobar/**",
		"/asignaciones/*/rechazar", "/asignaciones/*/rechazar/**"};

	public void configurar(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/css/**", "/js/**", "/img/**", "/vendor/bootstrap/**", "/favicon.ico", "/error", "/login").permitAll()
			.requestMatchers("/acceso-denegado").authenticated()
			.requestMatchers(lectura("/", "/dashboard")).hasAnyRole(TODOS)
			.requestMatchers("/admin/**").hasRole("ADMIN")
			// Las pantallas de modificación también se protegen, aunque se soliciten por GET.
			.requestMatchers(lectura("/menus/nuevo", "/menus/nuevo/**", "/menus/*/editar", "/menus/*/editar/**",
				"/dietas/nuevo", "/dietas/nuevo/**", "/dietas/*/editar", "/dietas/*/editar/**"))
				.hasAnyRole(NUTRICION)
			.requestMatchers(lectura("/pacientes/nuevo", "/pacientes/nuevo/**", "/pacientes/*/editar", "/pacientes/*/editar/**")).hasAnyRole("ADMIN", "MEDICO")
			.requestMatchers(lectura("/ubicaciones/nuevo", "/ubicaciones/nuevo/**", "/ubicaciones/*/editar", "/ubicaciones/*/editar/**")).hasAnyRole("ADMIN", "ENCARGADO_PISO")
			.requestMatchers(porMetodos(Set.of("GET", "HEAD", "POST"), APROBACIONES)).hasAnyRole(NUTRICION)
			.requestMatchers(APROBACIONES).denyAll()
			.requestMatchers(lectura("/reportes/pacientes", "/reportes/pacientes/**", "/reportes/ocupacion", "/reportes/ocupacion/**"))
				.hasAnyRole(CLINICOS_Y_PISO)
			.requestMatchers(lectura("/reportes/asignaciones", "/reportes/asignaciones/**", "/reportes/menus", "/reportes/menus/**"))
				.hasAnyRole(TODOS)
			.requestMatchers(lectura("/pacientes/**", "/ubicaciones/**")).hasAnyRole(CLINICOS_Y_PISO)
			.requestMatchers(lectura("/menus/**", "/dietas/**")).hasAnyRole(TODOS)
			.requestMatchers(lectura("/asignaciones/**")).hasAnyRole(ASIGNACIONES)
			.requestMatchers(porMetodos(Set.of("POST", "PUT", "PATCH"), "/pacientes/**")).hasAnyRole("ADMIN", "MEDICO")
			.requestMatchers(escritura("/menus/**", "/dietas/**")).hasAnyRole(NUTRICION)
			.requestMatchers(escritura("/ubicaciones/**")).hasAnyRole("ADMIN", "ENCARGADO_PISO")
			.requestMatchers(HttpMethod.POST, "/asignaciones/**").hasAnyRole(ASIGNACIONES)
			.requestMatchers(HttpMethod.PUT, "/asignaciones/**").hasAnyRole(ASIGNACIONES)
			.requestMatchers(HttpMethod.PATCH, "/asignaciones/**").hasAnyRole(ASIGNACIONES)
			.anyRequest().denyAll());
	}

	private RequestMatcher lectura(String... rutas) {
		return porMetodos(Set.of("GET", "HEAD"), rutas);
	}

	private RequestMatcher escritura(String... rutas) {
		return porMetodos(Set.of("POST", "PUT", "PATCH", "DELETE"), rutas);
	}

	private RequestMatcher porMetodos(Set<String> metodos, String... rutas) {
		var paths = Arrays.stream(rutas).<RequestMatcher>map(ruta -> PathPatternRequestMatcher.withDefaults().matcher(ruta)).toList();
		return new AndRequestMatcher(request -> metodos.contains(request.getMethod()), new OrRequestMatcher(paths));
	}
}
