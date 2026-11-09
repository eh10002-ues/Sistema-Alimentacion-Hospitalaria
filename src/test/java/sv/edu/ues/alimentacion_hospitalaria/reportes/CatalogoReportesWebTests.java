package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.OpcionReporteVista;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.TipoReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.CatalogoReportesService;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

/** HU-02 paso 2: portada real y catálogo sin consultas de pacientes. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CatalogoReportesWebTests {
    @Autowired MockMvc mvc;
    @Autowired CatalogoReportesService catalogo;

    @ParameterizedTest
    @EnumSource(Rol.class)
    void cadaRolRecibeSoloLosEnlacesAutorizados(Rol rol) throws Exception {
        for (String ruta : new String[]{"/reportes", "/reportes/"}) {
            var resultado = mvc.perform(get(ruta).with(user("prueba").roles(rol.name().substring(5))))
                .andExpect(status().isOk()).andExpect(view().name("reportes/inicio")).andReturn();
            var opciones = (List<?>) resultado.getModelAndView().getModel().get("reportes");
            var tipos = opciones.stream().map(opcion -> ((OpcionReporteVista) opcion).tipo()).toList();
            if (rol == Rol.ROLE_COCINA) {
                assertThat(tipos).containsExactly(TipoReporte.ASIGNACIONES, TipoReporte.MENUS);
            } else {
                assertThat(tipos).containsExactly(TipoReporte.values());
            }
            String html = resultado.getResponse().getContentAsString();
            assertThat(html).contains("Consultar reporte", "href=\"/reportes/asignaciones\"", "href=\"/reportes/menus\"")
                .doesNotContain("Pendiente de habilitar");
            if (rol == Rol.ROLE_COCINA) {
                assertThat(html).doesNotContain("href=\"/reportes/pacientes\"", "href=\"/reportes/ocupacion\"");
            } else {
                assertThat(html).contains("href=\"/reportes/pacientes\"", "href=\"/reportes/ocupacion\"");
            }
            mvc.perform(head(ruta).with(user("prueba").roles(rol.name().substring(5))))
                .andExpect(status().isOk());
        }
    }

    @Test
    void portadaRequiereSesionYUnRolAutorizado() throws Exception {
        mvc.perform(get("/reportes")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/reportes").with(user("otro").roles("OTRO"))).andExpect(status().isForbidden());
        mvc.perform(get("/reportes").with(user("multiple").roles("COCINA", "ADMIN")))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void portadaNoPermiteEscriturasAunqueLaPeticionTengaCsrf(Rol rol) throws Exception {
        for (HttpMethod metodo : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            mvc.perform(request(metodo, "/reportes").with(user("prueba").roles(rol.name().substring(5))).with(csrf()))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "OTRO")
    void servicioTambienProtegeElCatalogo() {
        assertThatThrownBy(() -> catalogo.consultar()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "COCINA")
    void servicioNoEntregaReportesGeneralesDePacientesAOcupacionACocina() {
        assertThat(catalogo.consultar()).extracting(OpcionReporteVista::tipo)
            .containsExactly(TipoReporte.ASIGNACIONES, TipoReporte.MENUS);
    }
}
