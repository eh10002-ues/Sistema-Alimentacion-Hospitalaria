package sv.edu.ues.alimentacion_hospitalaria.reportes.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

/** Mantiene el contrato de exactamente un rol, compartido por catálogo y consultas. */
@Component
public class AccesoReportes {
    public Rol rolActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        var roles = autenticacion.getAuthorities().stream()
            .map(autoridad -> autoridad.getAuthority()).filter(autoridad -> autoridad.startsWith("ROLE_")).toList();
        if (roles.size() != 1) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        try {
            return Rol.valueOf(roles.getFirst());
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Acceso denegado.");
        }
    }
}
