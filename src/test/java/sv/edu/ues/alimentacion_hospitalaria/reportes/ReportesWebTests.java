package sv.edu.ues.alimentacion_hospitalaria.reportes;

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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
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
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteEnfermedad;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteMedicamento;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
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
class ReportesWebTests {

    private static final String ALERGIAS = "/reportes/alergias-severas";
    private static final String COMANDAS = "/reportes/comandas";
    private static final String RACIONES = "/reportes/raciones-servidas";
    private static final String PENDIENTES = "/reportes/asignaciones-pendientes";
    private static final String SIN_MENU = "/reportes/pacientes-sin-menu";
    private static final List<String> RUTAS = List.of(ALERGIAS, COMANDAS, RACIONES, PENDIENTES, SIN_MENU);
    private static final List<String> ROLES = List.of("ADMIN", "MEDICO", "NUTRICIONISTA", "ENCARGADO_PISO", "COCINA");

    @Autowired private MockMvc mvc;
    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private MenuRepository menus;
    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;

    private Piso piso;
    private Menu menu;
    private Paciente conMenu;
    private Paciente pendiente;

    @BeforeEach
    void preparar() {
        DatosReportes datos = new DatosReportes(pisos, camas, pacientes, menus, asignaciones);
        Usuario nutricionista = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes)
            .usuario("nutri.reportes.web", Rol.ROLE_NUTRICIONISTA);
        LocalDate hoy = LocalDate.now(ZoneId.of("America/El_Salvador"));
        piso = datos.piso("Piso web");
        menu = datos.menu("Menú blando web", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        // Paciente con datos clínicos que no deben aparecer en las comandas.
        conMenu = datos.conAlergia(datos.paciente(piso, "Rosa", "Martínez"), "Mariscos", SeveridadAlergia.SEVERA);
        conMenu.agregarEnfermedad(new PacienteEnfermedad("Hipertensión arterial"));
        conMenu.agregarMedicamento(new PacienteMedicamento("Enalapril", "10 mg", (short) 12, LocalTime.of(8, 0)));
        pacientes.save(conMenu);
        datos.aprobada(conMenu, menu, nutricionista, hoy.minusDays(1), null);
        pendiente = datos.paciente(piso, "Tomás", "Pineda");
        datos.pendiente(pendiente, menu, nutricionista, hoy);
    }

    @ParameterizedTest
    @CsvSource({
        ALERGIAS + ", ADMIN|MEDICO|NUTRICIONISTA",
        COMANDAS + ", ADMIN|NUTRICIONISTA|COCINA",
        RACIONES + ", ADMIN|MEDICO|NUTRICIONISTA|ENCARGADO_PISO|COCINA",
        PENDIENTES + ", ADMIN|MEDICO|NUTRICIONISTA",
        SIN_MENU + ", ADMIN|MEDICO|NUTRICIONISTA"
    })
    void cadaReporteRespetaLosRolesAutorizados(String ruta, String permitidos) throws Exception {
        Set<String> autorizados = Set.of(permitidos.split("\\|"));
        for (String rol : ROLES) {
            var respuesta = mvc.perform(get(ruta).with(user("usuario").roles(rol)));
            if (autorizados.contains(rol)) {
                respuesta.andExpect(status().isOk());
            } else {
                respuesta.andExpect(status().isForbidden()).andExpect(forwardedUrl("/acceso-denegado"));
            }
        }
    }

    @Test
    void losReportesSonSoloDeLecturaYRequierenAutenticacion() throws Exception {
        for (String ruta : RUTAS) {
            mvc.perform(post(ruta).with(csrf()).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
            mvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "ADMIN, alergias-severas|comandas|raciones-servidas|asignaciones-pendientes|pacientes-sin-menu",
        "MEDICO, alergias-severas|raciones-servidas|asignaciones-pendientes|pacientes-sin-menu",
        "NUTRICIONISTA, alergias-severas|comandas|raciones-servidas|asignaciones-pendientes|pacientes-sin-menu",
        "COCINA, comandas|raciones-servidas",
        "ENCARGADO_PISO, raciones-servidas"
    })
    void elMenuLateralSoloMuestraLosReportesAutorizados(String rol, String visibles) throws Exception {
        String nombre = "reportes." + rol.toLowerCase();
        usuarios.save(new Usuario(nombre, "{noop}no-se-usa", "Usuario reportes", Rol.valueOf("ROLE_" + rol), true));
        String html = mvc.perform(get("/dashboard").with(user(nombre).roles(rol)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Set<String> esperados = visibles.isEmpty() ? Set.of() : Set.of(visibles.split("\\|"));
        for (String ruta : RUTAS) {
            String enlace = "href=\"" + ruta + "\"";
            if (esperados.contains(ruta.substring("/reportes/".length()))) {
                assertThat(html).contains(enlace);
            } else {
                assertThat(html).doesNotContain(enlace);
            }
        }
        if (esperados.isEmpty()) {
            assertThat(html).doesNotContain("REPORTES");
        }
    }

    @Test
    void racionesServidasResumeMensualmenteLasComidasDeAsignacionesAprobadas() throws Exception {
        String html = mvc.perform(get(RACIONES).with(user("nutricionista").roles("NUTRICIONISTA")))
            .andExpect(status().isOk())
            .andExpect(view().name("reportes/raciones-servidas"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("Raciones servidas por piso", menu.getNombre(), "Piso web", "Raciones estimadas")
            .doesNotContain("Mariscos", "Hipertensión arterial", "Enalapril");
    }

    @Test
    void comandasMuestranRacionesSinHorasNiDatosClinicosParaCocina() throws Exception {
        String html = mvc.perform(get(COMANDAS).param("pisoId", piso.getId().toString())
                .with(user("cocina").roles("COCINA")))
            .andExpect(status().isOk())
            .andExpect(view().name("reportes/comandas"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("Comandas de producción por tiempo de comida", "Desayuno", "Almuerzo", "Cena",
                "Avena " + menu.getNombre(), "Blanda", "Subtotal Desayuno", "Total general: 3 raciones")
            .doesNotContain("Mariscos", "Hipertensión", "Enalapril", "Grave")
            .doesNotContain(conMenu.getCodigo(), "Rosa Martínez")
            // Horas de servicio de prueba (cena 05:00, almuerzo 13:00, desayuno 21:00) como contenido de celda.
            .doesNotContain(">05:00", ">13:00", ">21:00");
    }

    @Test
    void alergiasSeverasMuestranLaEtiquetaGraveYLaNotaDeAlcance() throws Exception {
        String html = mvc.perform(get(ALERGIAS).param("pisoId", piso.getId().toString())
                .with(user("medico").roles("MEDICO")))
            .andExpect(status().isOk())
            .andExpect(view().name("reportes/alergias-severas"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("Pacientes con alergias severas registradas por piso", conMenu.getCodigo(), "Rosa Martínez",
                "Mariscos", "Grave", "El sistema no distingue el tipo de alergia")
            .contains("data-reporte-codigo=\"RE-01\"", "data-reporte-tabla", "id=\"exportar-pdf\"")
            .contains("/vendor/exportacion/jspdf.umd.min.js", "/vendor/exportacion/jspdf.plugin.autotable.min.js",
                "/js/reportes-pdf.js")
            .doesNotContain("Enalapril", "Hipertensión");
    }

    @Test
    void asignacionesPendientesAclaranQueNoSonPrescripciones() throws Exception {
        String html = mvc.perform(get(PENDIENTES).with(user("nutricionista").roles("NUTRICIONISTA")))
            .andExpect(status().isOk())
            .andExpect(view().name("reportes/asignaciones-pendientes"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("Asignaciones de menú pendientes de aprobación", pendiente.getCodigo(), "Tomás Pineda",
                menu.getNombre(), "Usuario nutri.reportes.web (Nutricionista)")
            .contains("No corresponde a prescripciones dietéticas pendientes")
            .doesNotContain(conMenu.getCodigo());
    }

    @Test
    void pacientesSinMenuIndicanLaAsignacionPendienteYSuCaracterDePropuesta() throws Exception {
        String html = mvc.perform(get(SIN_MENU).param("pisoId", piso.getId().toString())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("reportes/pacientes-sin-menu"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("Pacientes ingresados sin menú vigente aprobado", pendiente.getCodigo(), "Sí",
                "Sin asignaciones aprobadas", "PROPUESTA", "pendiente de validación del equipo")
            .doesNotContain(conMenu.getCodigo(), "Mariscos");
    }

    @Test
    void parametrosInvalidosSeRechazan() throws Exception {
        mvc.perform(get(COMANDAS).param("fecha", "15-03-2026").with(user("cocina").roles("COCINA")))
            .andExpect(status().isBadRequest());
        mvc.perform(get(COMANDAS).param("tiempoComida", "MERIENDA").with(user("cocina").roles("COCINA")))
            .andExpect(status().isBadRequest());
        mvc.perform(get(SIN_MENU).param("pisoId", "uno").with(user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
        mvc.perform(get(ALERGIAS).param("pisoId", "-1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound());
    }
}
