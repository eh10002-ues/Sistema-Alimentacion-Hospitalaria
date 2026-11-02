package sv.edu.ues.alimentacion_hospitalaria.inicio;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

@Service
public class DashboardService {
	public PanelRol construir(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Acceso denegado.");
		}
		var roles = Arrays.stream(Rol.values()).filter(rol -> authentication.getAuthorities().stream()
			.anyMatch(authority -> authority.getAuthority().equals(rol.name()))).toList();
		if (roles.size() != 1) {
			throw new AccessDeniedException("La cuenta debe tener un único rol autorizado.");
		}
		Rol rol = roles.getFirst();
		return switch (rol) {
			case ROLE_ADMIN -> new PanelRol(authentication.getName(), rol, "Administración de usuarios",
				"Administración de cuentas y acceso general al sistema.",
				List.of("Crear y mantener cuentas de usuario", "Asignar roles y controlar el acceso", "Consultar los cuatro reportes generales"),
				List.of(new PanelRol.Acceso("Administrar usuarios", "/admin/usuarios")));
			case ROLE_MEDICO -> new PanelRol(authentication.getName(), rol, "Pacientes y asignaciones de dieta",
				"Seguimiento de la alimentación y las restricciones de los pacientes.",
				List.of("Registrar y actualizar pacientes, alergias y restricciones", "Solicitar o asignar dietas", "Consultar los cuatro reportes generales"), List.of());
			case ROLE_NUTRICIONISTA -> new PanelRol(authentication.getName(), rol, "Asignaciones, dietas y menús",
				"Gestión de la alimentación y validación de asignaciones pendientes.",
				List.of("Administrar dietas, menús y tiempos de comida", "Aprobar o rechazar asignaciones", "Consultar los cuatro reportes generales"), List.of());
			case ROLE_ENCARGADO_PISO -> new PanelRol(authentication.getName(), rol, "Ocupación y ubicación hospitalaria",
				"Organización de pisos, habitaciones, camas y ubicación de los pacientes.",
				List.of("Gestionar ubicaciones hospitalarias", "Consultar asignaciones activas mediante su reporte", "Consultar los cuatro reportes generales"), List.of());
			case ROLE_COCINA -> new PanelRol(authentication.getName(), rol, "Menús y dietas del día",
				"Información operativa para preparar y distribuir alimentos.",
				List.of("Consultar menús aprobados y tiempos de comida", "Consultar el reporte de asignaciones activas", "Consultar el reporte de menús programados"), List.of());
		};
	}
}
