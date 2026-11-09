package sv.edu.ues.alimentacion_hospitalaria.security;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RevocadorSesiones {
	private final SessionRegistry sesiones;

	public RevocadorSesiones(SessionRegistry sesiones) {
		this.sesiones = sesiones;
	}

	public void despuesDeConfirmar(String nombreUsuario) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				for (Object principal : sesiones.getAllPrincipals()) {
					if (principal instanceof UserDetails usuario && usuario.getUsername().equalsIgnoreCase(nombreUsuario)) {
						sesiones.getAllSessions(principal, false).forEach(sesion -> sesion.expireNow());
					}
				}
			}
		});
	}
}
