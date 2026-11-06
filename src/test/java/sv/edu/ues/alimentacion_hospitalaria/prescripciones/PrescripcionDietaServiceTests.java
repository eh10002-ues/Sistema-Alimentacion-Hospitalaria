package sv.edu.ues.alimentacion_hospitalaria.prescripciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.EstadoPrescripcion;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.RegimenDietetico;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.persistence.PrescripcionDietaRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.service.PrescripcionAutorizada;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.service.PrescripcionDietaService;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class PrescripcionDietaServiceTests {

    @Autowired private PrescripcionDietaService servicio;
    @Autowired private PrescripcionDietaRepository prescripciones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;
    @Autowired private PacienteRepository pacientes;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationEvents eventos;

    private DatosClinicosPrueba datos;

    @BeforeEach
    void preparar() {
        datos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ROLE_MEDICO", "ROLE_NUTRICIONISTA"})
    void medicoONutricionistaAutorizaYLaPrescripcionQuedaAprobada(Rol rol) {
        Usuario autorizador = datos.usuario("autorizador.sah2", rol);
        Paciente paciente = datos.pacienteIngresado();
        comoUsuario(autorizador);

        var vista = servicio.autorizar(paciente.getId(), RegimenDietetico.BLANDA);
        entityManager.flush();
        entityManager.clear();

        PrescripcionDieta guardada = prescripciones.findById(vista.id()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoPrescripcion.APROBADO);
        assertThat(guardada.getRegimen()).isEqualTo(RegimenDietetico.BLANDA);
        assertThat(guardada.getAutorizadoPor().getId()).isEqualTo(autorizador.getId());
        assertThat(guardada.getPaciente().getId()).isEqualTo(paciente.getId());
        assertThat(guardada.getCreatedBy()).isEqualTo("autorizador.sah2");
        assertThat(vista.estado()).isEqualTo("Aprobado");
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM bitacora
            WHERE accion = 'APROBAR' AND tabla_afectada = 'prescripciones_dieta' AND id_afectado = ? AND usuario_id = ?""",
            Integer.class, vista.id(), autorizador.getId())).isEqualTo(1);
    }

    @Test
    void publicaLaOrdenParaCocinaSinDatosClinicos() {
        Usuario medico = datos.usuario("medico.evento", Rol.ROLE_MEDICO);
        Paciente paciente = datos.pacienteIngresado();
        comoUsuario(medico);

        var vista = servicio.autorizar(paciente.getId(), RegimenDietetico.CORRIENTE);

        assertThat(eventos.stream(PrescripcionAutorizada.class)).singleElement().satisfies(evento -> {
            var orden = evento.orden();
            assertThat(orden.prescripcionId()).isEqualTo(vista.id());
            assertThat(orden.codigoPaciente()).isEqualTo(paciente.getCodigo());
            assertThat(orden.paciente()).isEqualTo("Ana Pérez");
            assertThat(orden.ubicacion()).contains("Piso de prueba", "Cama");
            assertThat(orden.regimen()).isEqualTo("Corriente");
            assertThat(orden.estado()).isEqualTo("Aprobado");
            assertThat(orden.toString()).doesNotContain("Mariscos", "Hipertensión");
        });
    }

    @Test
    void conservaElHistorialSinSobrescribirPrescripcionesAnteriores() {
        Usuario medico = datos.usuario("medico.historial", Rol.ROLE_MEDICO);
        Paciente paciente = datos.pacienteIngresado();
        comoUsuario(medico);

        servicio.autorizar(paciente.getId(), RegimenDietetico.CORRIENTE);
        servicio.autorizar(paciente.getId(), RegimenDietetico.HIPOSODICA);
        entityManager.flush();
        entityManager.clear();

        var evaluacion = servicio.evaluar(paciente.getId());
        assertThat(evaluacion.historial()).extracting("regimen").containsExactly("Hiposódica", "Corriente");
        assertThat(evaluacion.historial()).extracting("estado").containsOnly("Aprobado");
        assertThat(evaluacion.alergias()).containsExactly("Mariscos (severa)");
        assertThat(evaluacion.enfermedades()).containsExactly("Hipertensión arterial");
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ROLE_ADMIN", "ROLE_ENCARGADO_PISO", "ROLE_COCINA"})
    void otrosRolesNoPuedenAutorizarNiConsultar(Rol rol) {
        Usuario usuario = datos.usuario("otro.rol", rol);
        Paciente paciente = datos.pacienteIngresado();
        comoUsuario(usuario);

        assertThatThrownBy(() -> servicio.autorizar(paciente.getId(), RegimenDietetico.BLANDA))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.evaluar(paciente.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId())).isEmpty();
        assertThat(eventos.stream(PrescripcionAutorizada.class)).isEmpty();
    }

    @Test
    void laCuentaInactivaNoPuedeAutorizarAunqueConserveLaSesion() {
        Usuario medico = datos.usuario("medico.inactivo", Rol.ROLE_MEDICO);
        medico.setActivo(false);
        Paciente paciente = datos.pacienteIngresado();
        comoUsuario(medico);

        assertThatThrownBy(() -> servicio.autorizar(paciente.getId(), RegimenDietetico.BLANDA))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("activo");
        assertThat(eventos.stream(PrescripcionAutorizada.class)).isEmpty();
    }

    @Test
    void elRolDeLaSesionDebeCoincidirConElRolVigenteEnLaBase() {
        Usuario usuario = datos.usuario("ahora.cocina", Rol.ROLE_COCINA);
        Paciente paciente = datos.pacienteIngresado();
        // La sesión conserva un rol anterior; el dominio valida el rol registrado.
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(usuario.getNombreUsuario(), null, Rol.ROLE_MEDICO.name()));

        assertThatThrownBy(() -> servicio.autorizar(paciente.getId(), RegimenDietetico.BLANDA))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaPacientesDeAltaYPacientesInexistentes() {
        Usuario medico = datos.usuario("medico.alta", Rol.ROLE_MEDICO);
        Paciente alta = datos.pacienteDeAlta();
        comoUsuario(medico);

        assertThatThrownBy(() -> servicio.autorizar(alta.getId(), RegimenDietetico.BLANDA))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ingresados");
        assertThatThrownBy(() -> servicio.autorizar(Long.MAX_VALUE, RegimenDietetico.BLANDA))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> servicio.autorizar(alta.getId(), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(eventos.stream(PrescripcionAutorizada.class)).isEmpty();
    }

    @Test
    void listaSoloPacientesIngresados() {
        Usuario nutricionista = datos.usuario("nutri.listado", Rol.ROLE_NUTRICIONISTA);
        Paciente ingresado = datos.pacienteIngresado();
        Paciente alta = datos.pacienteDeAlta();
        comoUsuario(nutricionista);

        assertThat(servicio.listarPacientesIngresados()).extracting("id")
            .contains(ingresado.getId()).doesNotContain(alta.getId());
    }

    @Test
    void sinAutenticacionNoSeAutorizaNiSeNotificaACocina() {
        Paciente paciente = datos.pacienteIngresado();
        SecurityContextHolder.clearContext();

        // A través del bean, @PreAuthorize rechaza la llamada antes de ejecutar el servicio.
        assertThatThrownBy(() -> servicio.autorizar(paciente.getId(), RegimenDietetico.BLANDA))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        // Sin el proxy de seguridad, el propio servicio también rechaza la falta de autenticación.
        PrescripcionDietaService sinProxy = AopTestUtils.getUltimateTargetObject(servicio);
        assertThatThrownBy(() -> sinProxy.autorizar(paciente.getId(), RegimenDietetico.BLANDA))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId())).isEmpty();
        assertThat(eventos.stream(PrescripcionAutorizada.class)).isEmpty();
    }

    @Test
    void usuarioAutenticadoNoRegistradoNoAutorizaNiNotificaACocina() {
        Paciente paciente = datos.pacienteIngresado();
        String nombre = "medico.no.registrado";
        assertThat(usuarios.findByNombreUsuarioIgnoreCase(nombre)).isEmpty();
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(nombre, null, Rol.ROLE_MEDICO.name()));

        assertThatThrownBy(() -> servicio.autorizar(paciente.getId(), RegimenDietetico.BLANDA))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId())).isEmpty();
        assertThat(eventos.stream(PrescripcionAutorizada.class)).isEmpty();
    }

    private void comoUsuario(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(usuario.getNombreUsuario(), null, usuario.getRol().name()));
    }
}
