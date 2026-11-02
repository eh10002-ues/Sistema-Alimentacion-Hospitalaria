package sv.edu.ues.alimentacion_hospitalaria.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class UsuarioRepositoryTests {

	@Autowired
	private UsuarioRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder encoder;

	@ParameterizedTest
	@EnumSource(Rol.class)
	void persisteCadaRolComoTextoYNormalizaElNombre(Rol rol) {
		Usuario usuario = guardar("Usuario.DEMO", rol);
		entityManager.clear();
		Usuario recuperado = repository.findByNombreUsuarioIgnoreCase("USUARIO.demo").orElseThrow();
		assertThat(recuperado.getId()).isEqualTo(usuario.getId());
		assertThat(recuperado.getNombreUsuario()).isEqualTo("usuario.demo");
		assertThat(recuperado.getRol()).isEqualTo(rol);
		assertThat(recuperado.isActivo()).isTrue();
		assertThat(jdbc.queryForObject("SELECT rol FROM usuarios WHERE id = ?", String.class, usuario.getId()))
			.isEqualTo(rol.name());
		assertThat(repository.existsByNombreUsuarioIgnoreCase("USUARIO.DEMO")).isTrue();
	}

	@Test
	void conservaHashYAuditoriaAlActualizarSinEliminarElUsuario() {
		Usuario usuario = guardar("medico.demo", Rol.ROLE_MEDICO);
		entityManager.refresh(usuario);
		String hash = usuario.getContrasenaHash();
		assertThat(usuario.getCreatedAt()).isNotNull();
		assertThat(usuario.getCreatedBy()).isEqualTo("system");
		assertThat(usuario.getUpdatedAt()).isNotNull();
		assertThat(usuario.getUpdatedBy()).isEqualTo("system");
		var fechaCreacion = usuario.getCreatedAt();
		usuario.setNombreCompleto("Médico de demostración actualizado");
		usuario.setActivo(false);
		entityManager.flush();
		entityManager.clear();
		Usuario recuperado = repository.findById(usuario.getId()).orElseThrow();
		assertThat(recuperado.isActivo()).isFalse();
		assertThat(recuperado.getNombreCompleto()).isEqualTo("Médico de demostración actualizado");
		assertThat(recuperado.getContrasenaHash()).isEqualTo(hash);
		assertThat(recuperado.getCreatedAt()).isEqualTo(fechaCreacion);
		assertThat(recuperado.getUpdatedAt()).isAfterOrEqualTo(recuperado.getCreatedAt());
	}

	@Test
	void rechazaDuplicadosInclusoConMayusculasDiferentes() {
		guardar("medico.demo", Rol.ROLE_MEDICO);
		assertThatThrownBy(() -> guardar("MEDICO.DEMO", Rol.ROLE_COCINA))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void rechazaDuplicadoAlModificarElNombre() {
		guardar("medico.demo", Rol.ROLE_MEDICO);
		Usuario otro = guardar("cocina.demo", Rol.ROLE_COCINA);
		otro.setNombreUsuario("MEDICO.DEMO");
		assertThatThrownBy(() -> entityManager.flush())
			.isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "medico demo", " medico", "medico ", "medico\tdemo", "medico\u00a0demo"})
	void rechazaNombresVaciosOConEspacios(String nombre) {
		assertThatThrownBy(() -> guardar(nombre, Rol.ROLE_MEDICO))
			.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void rechazaUsuarioSinRol() {
		assertThatThrownBy(() -> guardar("sin.rol", null))
			.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void consultasVaciasYPaginacionFuncionan() {
		assertThat(repository.findByNombreUsuarioIgnoreCase("inexistente")).isEmpty();
		assertThat(repository.existsByNombreUsuarioIgnoreCase("inexistente")).isFalse();
		guardar("b.demo", Rol.ROLE_COCINA);
		guardar("a.demo", Rol.ROLE_MEDICO);
		var pagina = repository.findAll(PageRequest.of(0, 1, Sort.by("nombreUsuario", "id")));
		assertThat(pagina.getTotalElements()).isEqualTo(2);
		assertThat(pagina.getContent()).extracting(Usuario::getNombreUsuario).containsExactly("a.demo");
	}

	@Test
	void baseDeDatosRechazaRolesFueraDelContrato() {
		Usuario usuario = guardar("medico.demo", Rol.ROLE_MEDICO);
		assertThatThrownBy(() -> jdbc.update("UPDATE usuarios SET rol = 'ROLE_OTRO' WHERE id = ?", usuario.getId()))
			.isInstanceOf(org.springframework.dao.DataAccessException.class)
			.hasMessageContaining("ck_usuarios_rol");
	}

	@ParameterizedTest
	@ValueSource(strings = {"MEDICO.DEMO", "medico demo", ""})
	void baseDeDatosRechazaNombresSinNormalizarAunqueSeOmitaJpa(String nombre) {
		Usuario usuario = guardar("medico.demo", Rol.ROLE_MEDICO);
		assertThatThrownBy(() -> jdbc.update("UPDATE usuarios SET nombre_usuario = ? WHERE id = ?", nombre, usuario.getId()))
			.isInstanceOf(org.springframework.dao.DataAccessException.class)
			.hasMessageContaining("ck_usuarios_nombre_usuario");
	}

	private Usuario guardar(String nombre, Rol rol) {
		String hash = encoder.encode(UUID.randomUUID().toString());
		Usuario usuario = repository.save(new Usuario(nombre, hash, "Persona de demostración", rol, true));
		entityManager.flush();
		return usuario;
	}
}
