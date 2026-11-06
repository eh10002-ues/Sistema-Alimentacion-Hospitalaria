package sv.edu.ues.alimentacion_hospitalaria.prescripciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/**
 * Sin @Transactional: el envío a cocina ocurre después del commit real. Los datos
 * creados se eliminan al terminar para no afectar a otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class NotificacionCocinaTests {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;
    @Autowired private PacienteRepository pacientes;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> usuariosCreados = new ArrayList<>();
    private final List<Long> pacientesCreados = new ArrayList<>();
    private DatosClinicosPrueba datos;
    private Usuario medico;
    private Usuario cocina;

    @BeforeEach
    void preparar() {
        datos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        medico = registrar(datos.usuario("medico.tiempo.real", Rol.ROLE_MEDICO));
        cocina = registrar(datos.usuario("cocina.tiempo.real", Rol.ROLE_COCINA));
    }

    @AfterEach
    void limpiar() {
        for (Long paciente : pacientesCreados) {
            jdbc.update("DELETE FROM bitacora WHERE tabla_afectada = 'prescripciones_dieta' AND id_afectado IN "
                + "(SELECT id FROM prescripciones_dieta WHERE paciente_id = ?)", paciente);
            jdbc.update("DELETE FROM prescripciones_dieta WHERE paciente_id = ?", paciente);
            jdbc.update("DELETE FROM paciente_enfermedades WHERE paciente_id = ?", paciente);
            jdbc.update("DELETE FROM paciente_alergias WHERE paciente_id = ?", paciente);
            Long cama = jdbc.queryForObject("SELECT cama_id FROM pacientes WHERE id = ?", Long.class, paciente);
            jdbc.update("DELETE FROM pacientes WHERE id = ?", paciente);
            if (cama != null) {
                Long piso = jdbc.queryForObject("SELECT piso_id FROM camas WHERE id = ?", Long.class, cama);
                jdbc.update("DELETE FROM camas WHERE id = ?", cama);
                jdbc.update("DELETE FROM pisos WHERE id = ?", piso);
            }
        }
        usuariosCreados.forEach(id -> jdbc.update("DELETE FROM usuarios WHERE id = ?", id));
    }

    @Test
    void laOrdenAprobadaLlegaACocinaEnTiempoRealSinDatosClinicos() throws Exception {
        Paciente paciente = registrar(datos.pacienteIngresado());
        MvcResult canal = abrirCanalCocina();
        assertThat(canal.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("event:conectado");

        mvc.perform(post("/prescripciones/pacientes/{id}", paciente.getId()).param("regimen", "HIPOSODICA")
                .with(como(medico)).with(csrf()))
            .andExpect(status().is3xxRedirection());

        String recibido = canal.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(recibido).contains("event:prescripcion-aprobada", paciente.getCodigo(), "Ana Pérez",
            "\"regimen\":\"Hiposódica\"", "\"estado\":\"Aprobado\"", "Piso de prueba");
        assertThat(recibido).doesNotContain("Mariscos", "Hipertensión", "pesoKg", "medicamentos");
        assertThat(canal.getResponse().getContentType()).startsWith("text/event-stream");
    }

    @Test
    void unaAutorizacionRechazadaNoSeEnviaACocina() throws Exception {
        Paciente alta = registrar(datos.pacienteDeAlta());
        MvcResult canal = abrirCanalCocina();

        mvc.perform(post("/prescripciones/pacientes/{id}", alta.getId()).param("regimen", "BLANDA")
                .with(como(medico)).with(csrf()))
            .andExpect(status().isOk());

        assertThat(canal.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("prescripcion-aprobada");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM prescripciones_dieta WHERE paciente_id = ?",
            Integer.class, alta.getId())).isZero();
    }

    // EventSource siempre decodifica UTF-8; el tipo text/event-stream no declara charset.
    private MvcResult abrirCanalCocina() throws Exception {
        return mvc.perform(get("/cocina/ordenes/eventos").with(como(cocina)))
            .andExpect(request().asyncStarted()).andReturn();
    }

    private RequestPostProcessor como(Usuario usuario) {
        return user(usuario.getNombreUsuario()).authorities(new SimpleGrantedAuthority(usuario.getRol().name()));
    }

    private Usuario registrar(Usuario usuario) {
        usuariosCreados.add(usuario.getId());
        return usuario;
    }

    private Paciente registrar(Paciente paciente) {
        pacientesCreados.add(paciente.getId());
        return paciente;
    }
}
