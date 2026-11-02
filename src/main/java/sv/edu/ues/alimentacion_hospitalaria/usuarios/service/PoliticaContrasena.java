package sv.edu.ues.alimentacion_hospitalaria.usuarios.service;

import org.springframework.stereotype.Component;

@Component
public class PoliticaContrasena {

	public void validar(String nombreUsuario, String contrasena, String confirmacion) {
		if (contrasena == null || confirmacion == null || !contrasena.equals(confirmacion)) {
			throw new IllegalArgumentException("La contraseña y su confirmación son obligatorias y deben coincidir.");
		}
		int longitud = contrasena.codePointCount(0, contrasena.length());
		if (longitud < 12 || longitud > 72) {
			throw new IllegalArgumentException("La contraseña debe tener entre 12 y 72 caracteres.");
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
