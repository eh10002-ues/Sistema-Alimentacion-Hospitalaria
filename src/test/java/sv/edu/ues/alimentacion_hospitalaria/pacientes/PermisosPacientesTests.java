package sv.edu.ues.alimentacion_hospitalaria.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteMedicamento;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteDetalleVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.AlergiaForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.service.AdministracionPacientesService;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/** HU-01: privacidad clínica y autorización de lectura/escritura en pacientes. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class PermisosPacientesTests {
    @Autowired MockMvc mvc;
    @Autowired PacienteRepository pacientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired PisoRepository pisos;
    @Autowired CamaRepository camas;
    @Autowired AdministracionPacientesService servicio;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        paciente = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes).pacienteIngresado();
        paciente.agregarMedicamento(new PacienteMedicamento("Medicamento sintético", "10 mg", (short) 8,
            LocalTime.of(6, 0)));
        pacientes.saveAndFlush(paciente);
    }

    @Test
    void cocinaNoAccedeAPacientesNiPorUrlDirecta() throws Exception {
        for (String ruta : new String[]{"/pacientes", "/pacientes/" + paciente.getId(),
                "/pacientes/" + paciente.getId() + "/medicamentos",
                "/pacientes/pisos/1/camas-disponibles"}) {
            mvc.perform(get(ruta).with(user("cocina").roles("COCINA"))).andExpect(status().isForbidden());
            mvc.perform(head(ruta).with(user("cocina").roles("COCINA"))).andExpect(status().isForbidden());
        }
    }

    @Test
    void pacientesRequiereAutenticacion() throws Exception {
        mvc.perform(get("/pacientes")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_PISO")
    void pisoRecibeSoloDatosOperativosEnElModeloYEnElHtml() throws Exception {
        var detalle = mvc.perform(get("/pacientes/{id}", paciente.getId()))
            .andExpect(status().isOk()).andReturn();
        var vista = (PacienteDetalleVista) detalle.getModelAndView().getModel().get("paciente");
        assertThat(vista.codigo()).isEqualTo(paciente.getCodigo());
        assertThat(vista.ubicacion()).contains(paciente.getCama().getNumero());
        assertThat(vista.medicamentos()).isEmpty();
        assertThat(vista.enfermedades()).isEmpty();
        assertThat(vista.alergias()).isEmpty();
        assertThat(vista.fechaNacimiento()).isNull();
        assertThat(vista.edadExacta()).isNull();
        assertThat(vista.pesoKg()).isNull();
        assertThat(vista.alturaCm()).isNull();
        assertThat(detalle.getResponse().getContentAsString())
            .doesNotContain("Medicamento sintético", "Hipertensión arterial", "Mariscos", "Medicamentos", "Peso");
        var listado = mvc.perform(get("/pacientes")).andExpect(status().isOk()).andReturn();
        assertThat(listado.getResponse().getContentAsString()).doesNotContain(">Enfermedades<", ">Alergias<");
        var pagina = (org.springframework.data.domain.Page<?>) listado.getModelAndView().getModel().get("pagina");
        PacienteVista fila = (PacienteVista) pagina.getContent().getFirst();
        assertThat(fila.enfermedades()).isNull();
        assertThat(fila.alergias()).isNull();
        for (String recurso : new String[]{"alergias", "enfermedades", "medicamentos"}) {
            mvc.perform(get("/pacientes/{id}/{recurso}", paciente.getId(), recurso))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "NUTRICIONISTA")
    void nutricionistaConsultaClinicaSinFormulariosDeEscritura() throws Exception {
        String html = mvc.perform(get("/pacientes/{id}", paciente.getId()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Medicamento sintético", "Hipertensión arterial", "Mariscos");
        for (String recurso : new String[]{"alergias", "enfermedades", "medicamentos"}) {
            String consulta = mvc.perform(get("/pacientes/{id}/{recurso}", paciente.getId(), recurso))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(consulta).doesNotContain("/eliminar", "/editar", "Guardar cambios", "Agregar alergia",
                "Agregar enfermedad", "Agregar medicamento");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"NUTRICIONISTA", "ENCARGADO_PISO", "COCINA"})
    void rolesDeConsultaNoPuedenCrearEditarOEliminarClinica(String rol) throws Exception {
        for (String recurso : new String[]{"alergias", "enfermedades", "medicamentos"}) {
            for (String sufijo : new String[]{"", "/1/editar", "/1/eliminar"}) {
                for (var metodo : new org.springframework.http.HttpMethod[]{org.springframework.http.HttpMethod.POST,
                        org.springframework.http.HttpMethod.PUT, org.springframework.http.HttpMethod.PATCH,
                        org.springframework.http.HttpMethod.DELETE}) {
                    mvc.perform(request(metodo, "/pacientes/" + paciente.getId() + "/" + recurso + sufijo)
                            .with(user("consulta").roles(rol)).with(csrf()).param("nombre", "No autorizado"))
                        .andExpect(status().isForbidden());
                }
            }
        }
        assertThat(pacientes.findById(paciente.getId()).orElseThrow().getAlergias()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "MEDICO"})
    void administracionYMedicoConservanGestionClinicaConCsrf(String rol) throws Exception {
        String ruta = "/pacientes/" + paciente.getId() + "/enfermedades";
        String html = mvc.perform(get(ruta).with(user("editor").roles(rol)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Agregar enfermedad", "/eliminar", "/editar");
        mvc.perform(post(ruta).with(user("editor").roles(rol)).param("nombre", "Sin CSRF"))
            .andExpect(status().isForbidden());
        mvc.perform(post(ruta).with(user("editor").roles(rol)).with(csrf()).param("nombre", "Diagnóstico sintético"))
            .andExpect(status().is3xxRedirection());
        assertThat(paciente.getEnfermedades()).extracting(enfermedad -> enfermedad.getNombre())
            .contains("Diagnóstico sintético");
    }

    @Test
    @WithMockUser(roles = "NUTRICIONISTA")
    void servicioTambienRechazaLaEscrituraClinicaDeNutricion() {
        assertThatThrownBy(() -> servicio.agregarAlergia(paciente.getId(), new AlergiaForm()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "COCINA")
    void servicioNoEntregaDetalleClinicoACocina() {
        assertThatThrownBy(() -> servicio.consultar(paciente.getId())).isInstanceOf(AccessDeniedException.class);
    }
}
