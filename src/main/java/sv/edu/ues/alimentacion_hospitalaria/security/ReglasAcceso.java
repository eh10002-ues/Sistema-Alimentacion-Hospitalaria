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
	private static final String[] CLINICOS = {"ADMIN", "MEDICO", "NUTRICIONISTA"};
	private static final String[] NUTRICION = {"ADMIN", "NUTRICIONISTA"};
	// SAH-2: solo médicos y nutricionistas evalúan y autorizan dietas; el administrador no participa.
	private static final String[] PRESCRIPCION = {"MEDICO", "NUTRICIONISTA"};
	private static final String[] PROYECCION_INSUMOS = {"ADMIN", "NUTRICIONISTA", "COCINA"};
	// SAH-6: cocina consulta menús aprobados y tiempos de comida, la nutricionista los administra y el administrador supervisa.
	// El encargado de piso accede a las asignaciones activas mediante su reporte, no mediante estas consultas.
	private static final String[] CONSULTAS_COCINA = {"ADMIN", "NUTRICIONISTA", "COCINA"};
	private static final String[] APROBACIONES = {"/asignaciones/*/aprobar", "/asignaciones/*/aprobar/**",
		"/asignaciones/*/rechazar", "/asignaciones/*/rechazar/**"};

	public void configurar(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/css/**", "/js/**", "/img/**", "/vendor/bootstrap/**", "/vendor/exportacion/**",
				"/favicon.ico", "/error", "/login").permitAll()
			.requestMatchers("/acceso-denegado").authenticated()
			.requestMatchers(lectura("/", "/dashboard")).hasAnyRole(TODOS)
			//.requestMatchers("/admin/ubicaciones", "/admin/ubicaciones/**").hasAnyRole("ADMIN", "ENCARGADO_PISO")
			.requestMatchers("/admin/**").hasRole("ADMIN")
			// Las pantallas de modificación también se protegen, aunque se soliciten por GET.
			.requestMatchers(lectura("/menus/nuevo", "/menus/nuevo/**", "/menus/*/editar", "/menus/*/editar/**",
				"/dietas/nuevo", "/dietas/nuevo/**", "/dietas/*/editar", "/dietas/*/editar/**"))
				.hasAnyRole(NUTRICION)
			.requestMatchers(lectura("/pacientes/nuevo", "/pacientes/nuevo/**", "/pacientes/*/editar", "/pacientes/*/editar/**")).hasAnyRole("ADMIN", "MEDICO")
			.requestMatchers(lectura("/pacientes/*/alergias", "/pacientes/*/alergias/**",
				"/pacientes/*/enfermedades", "/pacientes/*/enfermedades/**",
				"/pacientes/*/medicamentos", "/pacientes/*/medicamentos/**")).hasAnyRole(CLINICOS)
			.requestMatchers(lectura("/ubicaciones/nuevo", "/ubicaciones/nuevo/**", "/ubicaciones/*/editar", "/ubicaciones/*/editar/**")).hasAnyRole("ADMIN", "ENCARGADO_PISO")
			.requestMatchers(porMetodos(Set.of("GET", "HEAD", "POST"), APROBACIONES)).hasAnyRole(NUTRICION)
			.requestMatchers(APROBACIONES).denyAll()
			.requestMatchers(lectura("/insumos", "/insumos/nuevo", "/insumos/*/editar"))
				.hasAnyRole(NUTRICION)
			.requestMatchers(porMetodos(Set.of("POST"), "/insumos", "/insumos/*", "/insumos/*/eliminar"))
				.hasAnyRole(NUTRICION)
			.requestMatchers(lectura("/reportes/pacientes", "/reportes/pacientes/**", "/reportes/ocupacion", "/reportes/ocupacion/**"))
				.hasAnyRole(CLINICOS_Y_PISO)
			.requestMatchers(lectura("/reportes/asignaciones", "/reportes/asignaciones/**", "/reportes/menus", "/reportes/menus/**"))
				.hasAnyRole(TODOS)
			.requestMatchers(lectura("/pacientes/**")).hasAnyRole(CLINICOS_Y_PISO)
			.requestMatchers(lectura("/ubicaciones/**")).hasAnyRole("ADMIN", "ENCARGADO_PISO")
			.requestMatchers(lectura("/insumos/**")).hasAnyRole(PROYECCION_INSUMOS)
			.requestMatchers(lectura("/menus/**", "/dietas/**")).hasAnyRole(TODOS)
			.requestMatchers(lectura("/asignaciones/**")).hasAnyRole(ASIGNACIONES)
			.requestMatchers(porMetodos(Set.of("POST", "PUT", "PATCH", "DELETE"),
				"/pacientes/*/alergias", "/pacientes/*/alergias/**",
				"/pacientes/*/enfermedades", "/pacientes/*/enfermedades/**",
				"/pacientes/*/medicamentos", "/pacientes/*/medicamentos/**")).hasAnyRole("ADMIN", "MEDICO")
			.requestMatchers(porMetodos(Set.of("POST", "PUT", "PATCH"), "/pacientes/**")).hasAnyRole("ADMIN", "MEDICO")
			.requestMatchers(escritura("/menus/**", "/dietas/**")).hasAnyRole(NUTRICION)
			.requestMatchers(escritura("/ubicaciones/**")).hasAnyRole("ADMIN", "ENCARGADO_PISO")
			.requestMatchers(HttpMethod.POST, "/asignaciones/**").hasAnyRole(ASIGNACIONES)
			.requestMatchers(HttpMethod.PUT, "/asignaciones/**").hasAnyRole(ASIGNACIONES)
			.requestMatchers(HttpMethod.PATCH, "/asignaciones/**").hasAnyRole(ASIGNACIONES)
			// SAH-2: evaluación y autorización de la dieta (solo consulta y POST; no se define edición ni eliminación).
			.requestMatchers(porMetodos(Set.of("GET", "HEAD", "POST"), "/prescripciones/**")).hasAnyRole(PRESCRIPCION)
			// SAH-2: recepción en tiempo real de las órdenes aprobadas.
			.requestMatchers(lectura("/cocina/ordenes", "/cocina/ordenes/**")).hasRole("COCINA")
			// SAH-6: consultas operativas de cocina (solo lectura).
			.requestMatchers(lectura("/cocina/consultas/**")).hasAnyRole(CONSULTAS_COCINA)
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
