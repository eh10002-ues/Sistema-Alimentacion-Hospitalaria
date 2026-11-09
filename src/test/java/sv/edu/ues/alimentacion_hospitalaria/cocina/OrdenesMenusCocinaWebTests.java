package sv.edu.ues.alimentacion_hospitalaria.cocina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/** Carga inicial de /cocina/ordenes con las asignaciones de menú (D4) aprobadas y vigentes. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class OrdenesMenusCocinaWebTests {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/El_Salvador"));

    @Autowired private MockMvc mvc;
    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private MenuRepository menus;
    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;

    private DatosConsultasCocina datos;
    private Usuario nutri;
    private Menu activo;

    @BeforeEach
    void preparar() {
        DatosClinicosPrueba clinicos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        datos = new DatosConsultasCocina(clinicos, pacientes, menus, asignaciones);
        nutri = clinicos.usuario("nutri.ordenes.menus", Rol.ROLE_NUTRICIONISTA);
        activo = datos.menu("Menú Validación D4", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
    }

    @Test
    void cargaSoloAsignacionesAprobadasVigentesSinDuplicados() throws Exception {
        Paciente aprobado = datos.pacienteIngresado("Lucía", "Aprobada");
        AsignacionMenu vigente = datos.aprobada(aprobado, activo, nutri, HOY.minusDays(1), null);
        Paciente pendiente = datos.pacienteIngresado("Pedro", "Pendiente");
        datos.pendiente(pendiente, activo, nutri, HOY);
        Paciente conMenuInactivo = datos.pacienteIngresado("Inés", "Inactivo");
        datos.aprobada(conMenuInactivo, datos.menu("Menú inactivo", TipoDieta.BLANDA, EstadoMenu.INACTIVO), nutri, HOY, null);
        Paciente futuro = datos.pacienteIngresado("Fabio", "Futuro");
        datos.aprobada(futuro, activo, nutri, HOY.plusDays(1), null);
        Paciente vencido = datos.pacienteIngresado("Vera", "Vencida");
        datos.aprobada(vencido, activo, nutri, HOY.minusDays(5), HOY.minusDays(1));
        Paciente deAlta = datos.pacienteIngresado("Alan", "Alta");
        datos.aprobada(deAlta, activo, nutri, HOY, null);
        deAlta.setEstado(EstadoPaciente.ALTA);
        pacientes.saveAndFlush(deAlta);

        String html = mvc.perform(get("/cocina/ordenes").with(cocina())).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Lucía Aprobada (" + aprobado.getCodigo() + ")", activo.getNombre(), "Blanda")
            .doesNotContain(pendiente.getCodigo(), conMenuInactivo.getCodigo(), futuro.getCodigo(),
                vencido.getCodigo(), deAlta.getCodigo(), "No hay menús aprobados vigentes hoy.")
            // Sin datos clínicos del paciente.
            .doesNotContain("Mariscos", "Hipertensión");
        assertThat(html.split("data-asignacion-id=\"" + vigente.getId() + "\"", -1)).hasSize(2);

        String json = mvc.perform(get("/cocina/ordenes/menus").with(cocina())).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json).contains("\"asignacionId\":" + vigente.getId(), aprobado.getCodigo())
            .doesNotContain(pendiente.getCodigo(), conMenuInactivo.getCodigo(), futuro.getCodigo(),
                vencido.getCodigo(), deAlta.getCodigo());
        assertThat(json.split(aprobado.getCodigo(), -1)).hasSize(2);
    }

    @Test
    void sinAsignacionesAprobadasMuestraElMensajeVacio() throws Exception {
        datos.pendiente(datos.pacienteIngresado("Pedro", "Pendiente"), activo, nutri, HOY);
        mvc.perform(get("/cocina/ordenes").with(cocina())).andExpect(status().isOk())
            .andExpect(result -> assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("No hay menús aprobados vigentes hoy."));
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ROLE_ADMIN", "ROLE_MEDICO", "ROLE_NUTRICIONISTA", "ROLE_ENCARGADO_PISO"})
    void soloCocinaConsultaLosMenusAprobados(Rol rol) throws Exception {
        mvc.perform(get("/cocina/ordenes/menus").with(user("otro").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(rol.name()))))
            .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor cocina() {
        return user("cocina.ordenes").roles("COCINA");
    }
}
