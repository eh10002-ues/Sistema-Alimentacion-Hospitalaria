package sv.edu.ues.alimentacion_hospitalaria.prescripciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.EstadoPrescripcion;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.RegimenDietetico;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.persistence.PrescripcionDietaRepository;
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
class PrescripcionesWebTests {

    @Autowired private MockMvc mvc;
    @Autowired private PrescripcionDietaRepository prescripciones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;
    @Autowired private PacienteRepository pacientes;

    private DatosClinicosPrueba datos;
    private Usuario medico;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        datos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        medico = datos.usuario("medico.web", Rol.ROLE_MEDICO);
        paciente = datos.pacienteIngresado();
    }

    @Test
    void listaPacientesIngresadosYMuestraElAccesoEnElMenu() throws Exception {
        mvc.perform(get("/prescripciones").with(como(medico)))
            .andExpect(status().isOk()).andExpect(view().name("prescripciones/listado"))
            .andExpect(content().string(containsString(paciente.getCodigo())))
            .andExpect(content().string(containsString("href=\"/prescripciones/pacientes/" + paciente.getId() + "\"")))
            .andExpect(content().string(containsString("Prescripción de dieta")));
    }

    @Test
    void laEvaluacionMuestraDatosDelPacienteYSoloLosTresRegimenes() throws Exception {
        mvc.perform(get("/prescripciones/pacientes/{id}", paciente.getId()).with(como(medico)))
            .andExpect(status().isOk()).andExpect(view().name("prescripciones/evaluacion"))
            .andExpect(content().string(containsString("Mariscos (severa)")))
            .andExpect(content().string(containsString("Hipertensión arterial")))
            .andExpect(content().string(containsString(">Corriente<")))
            .andExpect(content().string(containsString(">Blanda<")))
            .andExpect(content().string(containsString(">Hiposódica<")))
            .andExpect(content().string(containsString("value=\"CORRIENTE\"")))
            .andExpect(content().string(not(containsString("LIQUIDA"))))
            .andExpect(content().string(containsString(">Autorizar<")))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void autorizarCreaLaPrescripcionAprobadaYRedirigeConConfirmacion() throws Exception {
        mvc.perform(post("/prescripciones/pacientes/{id}", paciente.getId()).param("regimen", "CORRIENTE")
                .with(como(medico)).with(csrf()))
            .andExpect(redirectedUrl("/prescripciones/pacientes/" + paciente.getId()))
            .andExpect(flash().attribute("mensaje", containsString("Aprobado")));

        var historial = prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId());
        assertThat(historial).singleElement().satisfies(prescripcion -> {
            assertThat(prescripcion.getRegimen()).isEqualTo(RegimenDietetico.CORRIENTE);
            assertThat(prescripcion.getEstado()).isEqualTo(EstadoPrescripcion.APROBADO);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "LIQUIDA", "NORMAL", "desconocido"})
    void rechazaRegimenesVaciosONoPermitidos(String regimen) throws Exception {
        mvc.perform(post("/prescripciones/pacientes/{id}", paciente.getId()).param("regimen", regimen)
                .with(como(medico)).with(csrf()))
            .andExpect(status().isOk()).andExpect(view().name("prescripciones/evaluacion"))
            .andExpect(model().attributeHasFieldErrors("formulario", "regimen"));
        assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId())).isEmpty();
    }

    @Test
    void muestraElMotivoCuandoElPacienteYaNoEstaIngresado() throws Exception {
        Paciente alta = datos.pacienteDeAlta();
        mvc.perform(post("/prescripciones/pacientes/{id}", alta.getId()).param("regimen", "BLANDA")
                .with(como(medico)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Solo se puede prescribir dieta a pacientes ingresados.")));
        assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(alta.getId())).isEmpty();
    }

    @Test
    void ignoraCamposNoPermitidosEnElFormulario() throws Exception {
        Usuario otro = datos.usuario("nutri.otro", Rol.ROLE_NUTRICIONISTA);
        mvc.perform(post("/prescripciones/pacientes/{id}", paciente.getId()).param("regimen", "BLANDA")
                .param("autorizadoPor", String.valueOf(otro.getId())).param("estado", "OTRO")
                .with(como(medico)).with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId()))
            .singleElement().satisfies(p -> assertThat(p.getAutorizadoPor().getId()).isEqualTo(medico.getId()));
    }

    @Test
    void autorizarExigeCsrf() throws Exception {
        mvc.perform(post("/prescripciones/pacientes/{id}", paciente.getId()).param("regimen", "BLANDA").with(como(medico)))
            .andExpect(status().isForbidden());
        assertThat(prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(paciente.getId())).isEmpty();
    }

    @Test
    void pacienteInexistenteResponde404() throws Exception {
        mvc.perform(get("/prescripciones/pacientes/{id}", Long.MAX_VALUE).with(como(medico)))
            .andExpect(status().isNotFound());
    }

    @Test
    void cocinaVeSuPantallaYSuAccesoEnElMenu() throws Exception {
        Usuario cocina = datos.usuario("cocina.web", Rol.ROLE_COCINA);
        mvc.perform(get("/cocina/ordenes").with(como(cocina)))
            .andExpect(status().isOk()).andExpect(view().name("cocina/ordenes"))
            .andExpect(content().string(containsString("data-eventos=\"/cocina/ordenes/eventos\"")))
            .andExpect(content().string(containsString("href=\"/cocina/ordenes\"")))
            .andExpect(content().string(not(containsString("href=\"/prescripciones\""))));
    }

    private RequestPostProcessor como(Usuario usuario) {
        return user(usuario.getNombreUsuario()).authorities(new SimpleGrantedAuthority(usuario.getRol().name()));
    }
}
