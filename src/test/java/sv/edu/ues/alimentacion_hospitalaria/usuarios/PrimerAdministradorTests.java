package sv.edu.ues.alimentacion_hospitalaria.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.config.InicializacionAdministradorConfig;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.service.PrimerAdministradorService;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class PrimerAdministradorTests {

	@Autowired
	private PrimerAdministradorService servicio;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder encoder;
	@Autowired
	private MockMvc mvc;
	@Autowired
	private ApplicationContext contexto;

	@Test
	void creaAdministradorActivoConHashNormalizacionYLoginValido() throws Exception {
		String clave = claveSintetica();
		servicio.crearSiNoExiste("ADMIN.DEMO", clave, "Administrador de prueba");
		Usuario creado = usuarios.findByNombreUsuarioIgnoreCase("admin.demo").orElseThrow();
		assertThat(creado.getRol()).isEqualTo(Rol.ROLE_ADMIN);
		assertThat(creado.isActivo()).isTrue();
		assertThat(creado.getNombreUsuario()).isEqualTo("admin.demo");
		assertThat(creado.getNombreCompleto()).isEqualTo("Administrador de prueba");
		assertThat(creado.getCreatedAt()).isNotNull();
		assertThat(creado.getContrasenaHash()).isNotEqualTo(clave);
		assertThat(encoder.matches(clave, creado.getContrasenaHash())).isTrue();
		mvc.perform(formLogin().user("ADMIN.DEMO").password(clave))
			.andExpect(authenticated().withUsername("admin.demo"));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void administradorExistenteInclusoInactivoImpideCrearOModificarCuentas(boolean activo) {
		String clave = claveSintetica();
		String hash = encoder.encode(clave);
		Usuario existente = usuarios.save(new Usuario("admin.existente", hash, "Administrador existente", Rol.ROLE_ADMIN, activo));
		servicio.crearSiNoExiste(null, null, null);
		servicio.crearSiNoExiste("otro.admin", claveSintetica(), "Otro administrador");
		assertThat(usuarios.findAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
		Usuario conservado = usuarios.findById(existente.getId()).orElseThrow();
		assertThat(conservado.getContrasenaHash()).isEqualTo(hash);
		assertThat(conservado.isActivo()).isEqualTo(activo);
		assertThat(conservado.getNombreCompleto()).isEqualTo("Administrador existente");
	}

	@Test
	void repetirInicializacionNoCambiaLaContrasena() {
		String clave = claveSintetica();
		servicio.crearSiNoExiste("admin.demo", clave, "Administrador de prueba");
		String hash = usuarios.findByNombreUsuarioIgnoreCase("admin.demo").orElseThrow().getContrasenaHash();
		servicio.crearSiNoExiste("ADMIN.DEMO", claveSintetica(), "Otro nombre");
		assertThat(usuarios.findByNombreUsuarioIgnoreCase("admin.demo").orElseThrow().getContrasenaHash()).isEqualTo(hash);
		assertThat(usuarios.findAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
	}

	@ParameterizedTest
	@ValueSource(strings = {"usuario", "clave", "nombre"})
	void variablesAusentesFallanSinCrearCuenta(String ausente) {
		assertThatThrownBy(() -> servicio.crearSiNoExiste(
			ausente.equals("usuario") ? null : "admin.demo",
			ausente.equals("clave") ? null : claveSintetica(),
			ausente.equals("nombre") ? null : "Administrador de prueba"))
			.isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_ADMIN_");
		assertThat(usuarios.existsByRol(Rol.ROLE_ADMIN)).isFalse();
	}

	@ParameterizedTest(name = "Configuración inválida {index}")
	@ValueSource(strings = {"", " ", " admin.demo", "admin.demo ", "admin demo"})
	void rechazaIdentificadoresInvalidosSinIncluirValores(String nombre) {
		assertThatThrownBy(() -> servicio.crearSiNoExiste(nombre, claveSintetica(), "Administrador de prueba"))
			.isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Revise APP_ADMIN_USERNAME:");
		assertThat(usuarios.existsByRol(Rol.ROLE_ADMIN)).isFalse();
	}

	@Test
	void rechazaContrasenaInvalidaSinExponerlaNiEncadenarlaEnLaExcepcion() {
		String invalida = UUID.randomUUID().toString();
		assertThatThrownBy(() -> servicio.crearSiNoExiste("admin.demo", invalida, "Administrador de prueba"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageStartingWith("Revise APP_ADMIN_PASSWORD:")
			.hasNoCause()
			.satisfies(error -> assertThat(error.toString()).doesNotContain(invalida));
		assertThat(usuarios.existsByRol(Rol.ROLE_ADMIN)).isFalse();
	}

	@Test
	void rechazaColisionSinPromoverUnUsuarioExistente() {
		Usuario existente = usuarios.save(new Usuario("admin.demo", encoder.encode(claveSintetica()),
			"Médico de prueba", Rol.ROLE_MEDICO, true));
		assertThatThrownBy(() -> servicio.crearSiNoExiste("ADMIN.DEMO", claveSintetica(), "Administrador de prueba"))
			.isInstanceOf(IllegalStateException.class).hasMessageContaining("ya está registrado");
		assertThat(usuarios.findById(existente.getId()).orElseThrow().getRol()).isEqualTo(Rol.ROLE_MEDICO);
		assertThat(usuarios.existsByRol(Rol.ROLE_ADMIN)).isFalse();
	}

	@Test
	void inicializadorNoSeRegistraFueraDelPerfilDeInicializacion() {
		assertThat(contexto.getBeansOfType(InicializacionAdministradorConfig.class)).isEmpty();
		assertThat(usuarios.existsByRol(Rol.ROLE_ADMIN)).isFalse();
	}

	private String claveSintetica() {
		return "Aa1" + UUID.randomUUID();
	}
}
