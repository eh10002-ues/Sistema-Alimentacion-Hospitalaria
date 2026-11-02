package sv.edu.ues.alimentacion_hospitalaria.usuarios.service;

import org.springframework.stereotype.Component;

@Component
public class PoliticaContrasena {
	private static final int LONGITUD_MINIMA = 12;
	private static final int LONGITUD_MAXIMA = 72;

	public String descripcion() {
		return "Use entre " + LONGITUD_MINIMA + " y " + LONGITUD_MAXIMA
			+ " caracteres, con mayúscula, minúscula y número. No use su nombre de usuario ni espacios al inicio o al final. Los caracteres especiales son opcionales.";
	}

	public void validar(String nombreUsuario, String contrasena, String confirmacion) {
		if (contrasena == null || confirmacion == null || !contrasena.equals(confirmacion)) {
			throw new IllegalArgumentException("La contraseña y su confirmación son obligatorias y deben coincidir.");
		}
		int longitud = contrasena.codePointCount(0, contrasena.length());
		if (longitud < LONGITUD_MINIMA || longitud > LONGITUD_MAXIMA) {
			throw new IllegalArgumentException("La contraseña debe tener entre " + LONGITUD_MINIMA + " y " + LONGITUD_MAXIMA + " caracteres.");
		}
		if (!contrasena.codePoints().anyMatch(Character::isUpperCase)
			|| !contrasena.codePoints().anyMatch(Character::isLowerCase)
			|| !contrasena.codePoints().anyMatch(Character::isDigit)) {
			throw new IllegalArgumentException("La contraseña debe incluir mayúscula, minúscula y número.");
		}
		if (contrasena.equalsIgnoreCase(nombreUsuario)) {
			throw new IllegalArgumentException("La contraseña no puede ser igual al nombre de usuario.");
		}
		if (esEspacio(contrasena.codePointAt(0)) || esEspacio(contrasena.codePointBefore(contrasena.length()))) {
			throw new IllegalArgumentException("La contraseña no puede comenzar ni terminar con espacios.");
		}
	}

	private boolean esEspacio(int caracter) {
		return Character.isWhitespace(caracter) || Character.isSpaceChar(caracter);
	}
}
