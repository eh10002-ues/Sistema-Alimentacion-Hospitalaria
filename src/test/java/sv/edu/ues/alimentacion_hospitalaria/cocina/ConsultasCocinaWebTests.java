package sv.edu.ues.alimentacion_hospitalaria.cocina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
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

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ConsultasCocinaWebTests {

    private static final String PACIENTES_POR_MENU = "/cocina/consultas/pacientes-por-menu";
    private static final String MENUS_POR_PACIENTE = "/cocina/consultas/menus-por-paciente";

    @Autowired private MockMvc mvc;
    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private MenuRepository menus;
    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;

    private Menu menu;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        DatosClinicosPrueba clinicos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        DatosConsultasCocina datos = new DatosConsultasCocina(clinicos, pacientes, menus, asignaciones);
        LocalDate hoy = LocalDate.now(ZoneId.of("America/El_Salvador"));
        menu = datos.menu("Menú blando de prueba", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        paciente = datos.pacienteIngresado("Rosa", "Martínez");
        datos.aprobada(paciente, menu, clinicos.usuario("nutri.consultas.web", Rol.ROLE_NUTRICIONISTA),
            hoy.minusDays(1), null);
    }

    @ParameterizedTest
    @CsvSource({
        "ADMIN, true", "NUTRICIONISTA, true", "COCINA, true", "ENCARGADO_PISO, false", "MEDICO, false"
    })
    void lasConsultasRespetanLosRolesAutorizados(String rol, boolean permitido) throws Exception {
        for (String ruta : new String[] {PACIENTES_POR_MENU, MENUS_POR_PACIENTE}) {
            var respuesta = mvc.perform(get(ruta).with(user("usuario").roles(rol)));
            if (permitido) {
                respuesta.andExpect(status().isOk());
            } else {
                respuesta.andExpect(status().isForbidden()).andExpect(forwardedUrl("/acceso-denegado"));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"COCINA, true", "NUTRICIONISTA, true", "ADMIN, true", "ENCARGADO_PISO, false", "MEDICO, false"})
    void elMenuLateralSoloMuestraLasConsultasALosRolesAutorizados(String rol, boolean visible) throws Exception {
        usuarios.save(new Usuario(
            "menu." + rol.toLowerCase(), "{noop}no-se-usa", "Usuario menú", Rol.valueOf("ROLE_" + rol), true));
        String html = mvc.perform(get("/dashboard").with(user("menu." + rol.toLowerCase()).roles(rol)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        if (visible) {
            assertThat(html).contains("href=\"/cocina/consultas/pacientes-por-menu\"", "href=\"/cocina/consultas/menus-por-paciente\"");
        } else {
            assertThat(html).doesNotContain("/cocina/consultas");
        }
    }

    @Test
    void lasConsultasSonSoloDeLecturaYRequierenAutenticacion() throws Exception {
        mvc.perform(post(PACIENTES_POR_MENU).with(csrf()).with(user("cocina").roles("COCINA")))
            .andExpect(status().isForbidden());
        mvc.perform(post(MENUS_POR_PACIENTE).with(csrf()).with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
        mvc.perform(get(PACIENTES_POR_MENU)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void pacientesPorMenuMuestraPacientesInternosSinDatosClinicos() throws Exception {
        String html = mvc.perform(get(PACIENTES_POR_MENU).param("tipoDieta", "BLANDA")
                .with(user("cocina").roles("COCINA")))
            .andExpect(status().isOk())
            .andExpect(view().name("cocina/pacientes-por-menu"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains(menu.getNombre(), "Blanda", paciente.getCodigo(), "Rosa Martínez", paciente.getCama().getNumero())
            .contains("Pacientes por menú")
            // Datos clínicos del paciente sintético que no deben exponerse a cocina.
            .doesNotContain("Mariscos", "Hipertensión");
    }

    @Test
    void menusPorPacienteMuestraDietaActivaHorariosEHistorial() throws Exception {
        String html = mvc.perform(get(MENUS_POR_PACIENTE).param("pacienteId", paciente.getId().toString())
                .with(user("nutricionista").roles("NUTRICIONISTA")))
            .andExpect(status().isOk())
            .andExpect(view().name("cocina/menus-por-paciente"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("Rosa Martínez", menu.getNombre(), "Dieta activa", "Historial de menús")
            .contains("07:00", "Desayuno", "Avena con frutas", "12:30", "Almuerzo", "18:00", "Cena")
            .contains("Vigente")
            .doesNotContain("Mariscos", "Hipertensión");
    }

    @Test
    void regimenInvalidoOPacienteInexistenteSeRechazan() throws Exception {
        mvc.perform(get(PACIENTES_POR_MENU).param("tipoDieta", "INEXISTENTE").with(user("cocina").roles("COCINA")))
            .andExpect(status().isBadRequest());
        mvc.perform(get(MENUS_POR_PACIENTE).param("pacienteId", "-1").with(user("cocina").roles("COCINA")))
            .andExpect(status().isNotFound());
    }
}
