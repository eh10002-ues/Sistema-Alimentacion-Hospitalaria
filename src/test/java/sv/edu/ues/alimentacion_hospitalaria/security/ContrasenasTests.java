package sv.edu.ues.alimentacion_hospitalaria.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.PoliticaContrasena;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.ServicioContrasenas;

class ContrasenasTests {

	private final PoliticaContrasena politica = new PoliticaContrasena();

	@ParameterizedTest(name = "Caso inválido {index}")
	@MethodSource("contrasenasInvalidas")
	void rechazaReglasIncumplidasSinIncluirElValorEnElError(String contrasena) {
		assertThatThrownBy(() -> politica.validar("usuario.demo", contrasena, contrasena))
			.isInstanceOf(IllegalArgumentException.class)
			.satisfies(error -> {
				if (contrasena != null && !contrasena.isEmpty()) {
					assertThat(error.getMessage()).doesNotContain(contrasena);
				}
			});
	}

	static Stream<String> contrasenasInvalidas() {
		return Stream.of(null, "", "Aa1" + "x".repeat(8), "Aa1" + "x".repeat(70),
			"a1" + "x".repeat(10), "A1" + "X".repeat(10), "Aa" + "x".repeat(10),
			" Aa1" + "x".repeat(10), "Aa1" + "x".repeat(10) + " ",
			"\tAa1" + "x".repeat(10), "Aa1" + "x".repeat(10) + "\u00a0");
	}

	@Test
	void rechazaNombreDeUsuarioAunqueSeCambienMayusculas() {
		String valor = "Aa1" + UUID.randomUUID();
		assertThatThrownBy(() -> politica.validar(valor.toLowerCase(java.util.Locale.ROOT), valor, valor))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("La contraseña no puede ser igual al nombre de usuario.");
	}

	@Test
	void exigeConfirmacionIdentica() {
		String valor = "Aa1" + UUID.randomUUID();
		assertThatThrownBy(() -> politica.validar("usuario.demo", valor, null))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> politica.validar("usuario.demo", valor, valor + "a"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest(name = "Límite válido {index}")
	@MethodSource("contrasenasValidas")
	void admiteLimitesSinEspecialesYTextoUnicode(String valor) {
		assertThatCode(() -> politica.validar("usuario.demo", valor, valor)).doesNotThrowAnyException();
		var encoder = new SecurityConfig().passwordEncoder();
		var servicio = new ServicioContrasenas(politica, encoder);
		String hash = servicio.codificar("usuario.demo", valor, valor);
		assertThat(hash).startsWith("{pbkdf2@SpringSecurity_v5_8}").isNotEqualTo(valor);
		assertThat(encoder.matches(valor, hash)).isTrue();
	}

	static Stream<String> contrasenasValidas() {
		return Stream.of("Aa1" + "x".repeat(9), "Aa1" + "x".repeat(69),
			"Áñ1" + "é".repeat(69), "Aa1" + "😀".repeat(69), "Aa1" + "x x".repeat(4));
	}

	@Test
	void generaHashesDistintosConSalYRechazaUnaContrasenaDiferente() {
		var encoder = new SecurityConfig().passwordEncoder();
		var servicio = new ServicioContrasenas(politica, encoder);
		String valor = "Aa1" + UUID.randomUUID();
		String primero = servicio.codificar("usuario.demo", valor, valor);
		String segundo = servicio.codificar("usuario.demo", valor, valor);
		assertThat(primero).isNotEqualTo(segundo);
		assertThat(encoder.matches(valor + "a", primero)).isFalse();
	}

	@Test
	void servicioRechazaAntesDeCodificar() {
		var encoder = org.mockito.Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);
		var servicio = new ServicioContrasenas(politica, encoder);
		assertThatThrownBy(() -> servicio.codificar("usuario.demo", null, null))
			.isInstanceOf(IllegalArgumentException.class);
		org.mockito.Mockito.verifyNoInteractions(encoder);
	}
}
