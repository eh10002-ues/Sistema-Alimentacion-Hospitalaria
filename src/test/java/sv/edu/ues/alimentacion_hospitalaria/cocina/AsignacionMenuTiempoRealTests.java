package sv.edu.ues.alimentacion_hospitalaria.cocina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
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
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/**
 * Sin @Transactional: el aviso a cocina ocurre después del commit real de la aprobación D4.
 * Los datos creados se eliminan al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AsignacionMenuTiempoRealTests {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/El_Salvador"));
    private static final String EVENTO = "event:asignacion-menu-aprobada";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;
    @Autowired private PacienteRepository pacientes;
    @Autowired private MenuRepository menus;
    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> usuariosCreados = new ArrayList<>();
    private final List<Long> pacientesCreados = new ArrayList<>();
    private final List<Long> menusCreados = new ArrayList<>();
    private DatosConsultasCocina datos;
    private Usuario medico;
    private Usuario nutri;
    private Usuario cocina;
    private Menu menu;

    @BeforeEach
    void preparar() {
        DatosClinicosPrueba clinicos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        datos = new DatosConsultasCocina(clinicos, pacientes, menus, asignaciones);
        medico = registrar(clinicos.usuario("medico.menu.tiempo.real", Rol.ROLE_MEDICO));
        nutri = registrar(clinicos.usuario("nutri.menu.tiempo.real", Rol.ROLE_NUTRICIONISTA));
        cocina = registrar(clinicos.usuario("cocina.menu.tiempo.real", Rol.ROLE_COCINA));
        menu = datos.menu("Menú Validación D4", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        menusCreados.add(menu.getId());
    }

    @AfterEach
    void limpiar() {
        for (Long paciente : pacientesCreados) {
            jdbc.update("DELETE FROM asignaciones_menu WHERE paciente_id = ?", paciente);
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
        for (Long id : menusCreados) {
            jdbc.update("DELETE FROM menu_detalles WHERE menu_id = ?", id);
            jdbc.update("DELETE FROM menus WHERE id = ?", id);
        }
        for (Long id : usuariosCreados) {
            jdbc.update("DELETE FROM bitacora WHERE usuario_id = ?", id);
            jdbc.update("DELETE FROM usuarios WHERE id = ?", id);
        }
    }

    @Test
    void laAprobacionD4LlegaACocinaUnaSolaVezSinDatosClinicos() throws Exception {
        Paciente paciente = registrar(datos.pacienteIngresado("Sara", "Novena"));
        AsignacionMenu pendiente = datos.pendiente(paciente, menu, medico, HOY);
        MvcResult canal = abrirCanalCocina();
        assertThat(contenido(canal)).contains("event:conectado").doesNotContain(EVENTO);

        mvc.perform(post("/asignaciones/{id}/aprobar", pendiente.getId()).with(como(nutri)).with(csrf()))
            .andExpect(flash().attribute("mensaje", "Asignación aprobada."));

        String recibido = contenido(canal);
        assertThat(recibido).contains(EVENTO, "id:asignacion-" + pendiente.getId(), paciente.getCodigo(),
            "Sara Novena", menu.getNombre(), "\"regimen\":\"Blanda\"", "Piso de prueba");
        assertThat(recibido).doesNotContain("Mariscos", "Hipertensión", "prescripcion-aprobada");
        assertThat(recibido.split(EVENTO, -1)).hasSize(2);

        // Un segundo intento de aprobación falla y no vuelve a notificar.
        mvc.perform(post("/asignaciones/{id}/aprobar", pendiente.getId()).with(como(nutri)).with(csrf()))
            .andExpect(flash().attribute("error", "La asignación ya fue aprobada."));
        assertThat(contenido(canal).split(EVENTO, -1)).hasSize(2);
    }

    @Test
    void noSeNotificanAprobacionesRechazadasNiAsignacionesFuturas() throws Exception {
        Paciente paciente = registrar(datos.pacienteIngresado("Tomás", "Futuro"));
        AsignacionMenu futura = datos.pendiente(paciente, menu, medico, HOY.plusDays(2));
        MvcResult canal = abrirCanalCocina();

        // El médico no aprueba: 403 y sin aviso.
        mvc.perform(post("/asignaciones/{id}/aprobar", futura.getId()).with(como(medico)).with(csrf()))
            .andExpect(status().isForbidden());
        assertThat(contenido(canal)).doesNotContain(EVENTO);

        // Aprobada pero todavía no vigente: aparecerá en la carga inicial de su fecha.
        mvc.perform(post("/asignaciones/{id}/aprobar", futura.getId()).with(como(nutri)).with(csrf()))
            .andExpect(flash().attribute("mensaje", "Asignación aprobada."));
        assertThat(contenido(canal)).doesNotContain(EVENTO);
        assertThat(jdbc.queryForObject("SELECT aprobado_por_id FROM asignaciones_menu WHERE id = ?",
            Long.class, futura.getId())).isEqualTo(nutri.getId());
    }

    private MvcResult abrirCanalCocina() throws Exception {
        return mvc.perform(get("/cocina/ordenes/eventos").with(como(cocina)))
            .andExpect(request().asyncStarted()).andReturn();
    }

    // EventSource siempre decodifica UTF-8; el tipo text/event-stream no declara charset.
    private static String contenido(MvcResult canal) throws Exception {
        return canal.getResponse().getContentAsString(StandardCharsets.UTF_8);
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
