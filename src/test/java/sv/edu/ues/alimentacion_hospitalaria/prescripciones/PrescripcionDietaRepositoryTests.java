package sv.edu.ues.alimentacion_hospitalaria.prescripciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.EstadoPrescripcion;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.RegimenDietetico;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.persistence.PrescripcionDietaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class PrescripcionDietaRepositoryTests {

    @Autowired private PrescripcionDietaRepository prescripciones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;
    @Autowired private PacienteRepository pacientes;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private DatosClinicosPrueba datos;

    @BeforeEach
    void preparar() {
        datos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
    }

    @Test
    void conservaElHistorialOrdenadoDelMasRecienteAlMasAntiguo() {
        Usuario medico = datos.usuario("medico.repo", Rol.ROLE_MEDICO);
        Paciente paciente = datos.pacienteIngresado();
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        prescripciones.save(PrescripcionDieta.autorizar(paciente, RegimenDietetico.CORRIENTE, medico, ahora.minusSeconds(60)));
        prescripciones.save(PrescripcionDieta.autorizar(paciente, RegimenDietetico.HIPOSODICA, medico, ahora));
        entityManager.flush();
        entityManager.clear();

        var historial = prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId());

        assertThat(historial).extracting(PrescripcionDieta::getRegimen)
            .containsExactly(RegimenDietetico.HIPOSODICA, RegimenDietetico.CORRIENTE);
        assertThat(historial).allSatisfy(prescripcion -> {
            assertThat(prescripcion.getEstado()).isEqualTo(EstadoPrescripcion.APROBADO);
            assertThat(prescripcion.getCreatedBy()).isNotBlank();
        });
        assertThat(historial.getFirst().getFechaAutorizacion()).isEqualTo(ahora);
        // Corriente se guarda con el valor NORMAL compartido con el módulo de menús.
        assertThat(jdbc.queryForList("SELECT tipo_dieta FROM prescripciones_dieta WHERE paciente_id = ? ORDER BY id",
            String.class, paciente.getId())).containsExactly("NORMAL", "HIPOSODICA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIQUIDA", "HIPOCALORICA", "PAPILLA", "CORRIENTE"})
    void laBaseRechazaRegimenesFueraDeSah2(String tipoDieta) {
        Usuario medico = datos.usuario("medico.check", Rol.ROLE_MEDICO);
        Paciente paciente = datos.pacienteIngresado();
        entityManager.flush();

        assertThatThrownBy(() -> insertar(paciente.getId(), tipoDieta, "APROBADO", medico.getId()))
            .isInstanceOf(DataAccessException.class).hasMessageContaining("ck_presc_dieta_tipo");
    }

    @Test
    void laBaseRechazaEstadosNoDefinidos() {
        Usuario medico = datos.usuario("medico.estado", Rol.ROLE_MEDICO);
        Paciente paciente = datos.pacienteIngresado();
        entityManager.flush();

        assertThatThrownBy(() -> insertar(paciente.getId(), TipoDieta.BLANDA.name(), "PENDIENTE", medico.getId()))
            .isInstanceOf(DataAccessException.class).hasMessageContaining("ck_presc_dieta_estado");
    }

    private void insertar(Long pacienteId, String tipoDieta, String estado, Long usuarioId) {
        jdbc.update("""
            INSERT INTO prescripciones_dieta (paciente_id, tipo_dieta, estado, autorizado_por_id, fecha_autorizacion,
                created_at, created_by, updated_at, updated_by)
            VALUES (?, ?, ?, ?, NOW(6), NOW(6), 'prueba', NOW(6), 'prueba')""", pacienteId, tipoDieta, estado, usuarioId);
    }
}
