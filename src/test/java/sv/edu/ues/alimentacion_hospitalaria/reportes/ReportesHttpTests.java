package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.*;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/** HU-02 paso 4: rutas reales, privacidad del modelo y validación HTTP. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ReportesHttpTests {
    private static final List<String> REPORTES = List.of("pacientes", "asignaciones", "menus", "ocupacion");
    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired UsuarioRepository usuarios;
    @Autowired PisoRepository pisos;
    @Autowired CamaRepository camas;
    @Autowired PacienteRepository pacientes;
    @Autowired MenuRepository menus;
    @Autowired AsignacionMenuRepository asignaciones;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        var datos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        var responsable = datos.usuario("responsable.http", Rol.ROLE_MEDICO);
        paciente = datos.pacienteIngresado();
        Menu menu = new Menu("Menú sintético HTTP", TipoMenu.GENERAL, TipoDieta.BLANDA);
        menu.setEstado(EstadoMenu.ACTIVO);
        menu.agregarDetalle(new MenuDetalle(TiempoComida.ALMUERZO, LocalTime.of(12, 0), "Platillo sintético"));
        menus.save(menu);
        AsignacionMenu asignacion = new AsignacionMenu(paciente, menu, responsable, LocalDate.of(2026, 1, 1));
        asignacion.setAprobadoPor(responsable);
        asignacion.setFechaAprobacion(LocalDate.of(2026, 1, 1));
        asignaciones.saveAndFlush(asignacion);
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void matrizGetYHeadSeAplicaALasCuatroConsultasReales(Rol rol) throws Exception {
        for (String reporte : REPORTES) {
            boolean permitido = rol != Rol.ROLE_COCINA || reporte.equals("asignaciones") || reporte.equals("menus");
            for (String sufijo : List.of("", "/")) {
                String ruta = "/reportes/" + reporte + sufijo;
                var resultado = mvc.perform(get(ruta).with(user("prueba").roles(rol.name().substring(5))));
                if (permitido) {
                    var respuesta = resultado.andExpect(status().isOk()).andExpect(view().name("reportes/consulta")).andReturn();
                    Page<?> pagina = (Page<?>) respuesta.getModelAndView().getModel().get("pagina");
                    assertThat(pagina).hasSize(1);
                } else {
                    var respuesta = resultado.andExpect(status().isForbidden()).andReturn();
                    assertThat(respuesta.getModelAndView()).isNull();
                }
                mvc.perform(head(ruta).with(user("prueba").roles(rol.name().substring(5))))
                    .andExpect(permitido ? status().isOk() : status().isForbidden());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void asignacionesSoloEntregaLosCamposAutorizadosEnElModelo(Rol rol) throws Exception {
        var resultado = mvc.perform(get("/reportes/asignaciones").param("fecha", "2026-11-08")
                .with(user("prueba").roles(rol.name().substring(5))))
            .andExpect(status().isOk()).andReturn();
        Page<?> pagina = (Page<?>) resultado.getModelAndView().getModel().get("pagina");
        Object fila = pagina.getContent().getFirst();
        if (rol == Rol.ROLE_COCINA) {
            assertThat(fila).isInstanceOf(FilasReporte.AsignacionCocina.class);
        } else if (rol == Rol.ROLE_ENCARGADO_PISO) {
            assertThat(fila).isInstanceOf(FilasReporte.AsignacionPiso.class);
        } else { assertThat(fila).isInstanceOf(FilasReporte.AsignacionClinica.class); }
        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("<table", "Menú sintético HTTP", paciente.getCodigo());
        if (rol == Rol.ROLE_COCINA || rol == Rol.ROLE_ENCARGADO_PISO) {
            assertThat(html).doesNotContain("<th scope=\"col\">Responsable</th>", "<th scope=\"col\">Inicio</th>",
                "<th scope=\"col\">Fin</th>", "Usuario responsable.http");
        } else {
            assertThat(html).contains("<th scope=\"col\">Responsable</th>", "Usuario responsable.http", "Sin fecha de fin");
        }
        if (rol == Rol.ROLE_ENCARGADO_PISO) {
            assertThat(html).doesNotContain("<th scope=\"col\">Tiempos de comida</th>");
        } else { assertThat(html).contains("Almuerzo · 12:00"); }
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("Hipertensión", "Mariscos", "hash-no-utilizable");
        assertThat(resultado.getModelAndView().getModel().values())
            .noneMatch(valor -> valor instanceof jakarta.persistence.Tuple || valor instanceof Paciente);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pacientes", "asignaciones", "menus", "ocupacion"})
    void requiereSesionYRechazaRolesInvalidosYEscrituras(String reporte) throws Exception {
        String ruta = "/reportes/" + reporte;
        mvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
        mvc.perform(get(ruta).with(user("otro").roles("OTRO"))).andExpect(status().isForbidden());
        mvc.perform(get(ruta).param("pagina", "invalida").with(user("multiple").roles("ADMIN", "COCINA")))
            .andExpect(status().isForbidden());
        for (Rol rol : Rol.values()) {
            for (HttpMethod metodo : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
                mvc.perform(request(metodo, ruta).with(user("prueba").roles(rol.name().substring(5))).with(csrf()))
                    .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void filtrosInvalidosSon400ConMensajesComprensiblesSinDetallesTecnicos() throws Exception {
        String[][] casos = {
            {"pacientes", "pagina", "-1"}, {"pacientes", "pagina", "abc"}, {"pacientes", "pagina", "2147483648"},
            {"pacientes", "pisoId", "abc"}, {"pacientes", "pisoId", "0"}, {"pacientes", "estado", "INEXISTENTE"},
            {"pacientes", "buscar", "x".repeat(101)}, {"asignaciones", "fecha", "2026-02-30"},
            {"asignaciones", "tipoDieta", "INEXISTENTE"}, {"menus", "fecha", "no-es-fecha"},
            {"menus", "tiempoComida", "INEXISTENTE"}, {"ocupacion", "ocupacion", "INEXISTENTE"},
            {"ocupacion", "cama", "x".repeat(11)}, {"ocupacion", "pisoId", Long.toString(Long.MAX_VALUE)}
        };
        for (String[] caso : casos) {
            var resultado = mvc.perform(get("/reportes/" + caso[0]).param(caso[1], caso[2])
                    .with(user("prueba").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(view().name("reportes/filtros-invalidos")).andReturn();
            assertThat(resultado.getResponse().getContentAsString()).contains("Revise los filtros del reporte")
                .doesNotContain("java.lang", "org.springframework", "SELECT", "Exception", "stacktrace");
            if (caso[2].length() > 4) {
                assertThat(resultado.getResponse().getContentAsString()).doesNotContain(caso[2]);
            }
            assertThat(resultado.getModelAndView().getModel()).doesNotContainKey("pagina");
        }
    }

    @Test
    void filtrosValidosSeAplicanYListaVaciaSeDistingueDeErrores() throws Exception {
        var resultado = mvc.perform(get("/reportes/pacientes").param("buscar", "  " + paciente.getCodigo() + "  ")
                .param("pisoId", paciente.getCama().getPiso().getId().toString()).param("estado", "INGRESADO")
                .with(user("prueba").roles("ENCARGADO_PISO")))
            .andExpect(status().isOk()).andReturn();
        var filtros = (FiltrosReporte.Pacientes) resultado.getModelAndView().getModel().get("filtros");
        assertThat(filtros.buscar()).isEqualTo(paciente.getCodigo());
        Page<?> pagina = (Page<?>) resultado.getModelAndView().getModel().get("pagina");
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        var vacio = mvc.perform(get("/reportes/pacientes").param("buscar", "NO-EXISTE")
                .with(user("prueba").roles("MEDICO")))
            .andExpect(status().isOk()).andReturn();
        assertThat(vacio.getResponse().getContentAsString()).contains("No hay resultados para los filtros seleccionados.");
    }

    @Test
    void filtrosDeFechaDietaYTiempoSeAplicanEnLaPeticionHttp() throws Exception {
        var resultado = mvc.perform(get("/reportes/menus").param("fecha", "2026-11-08")
                .param("tipoDieta", "BLANDA").param("tiempoComida", "ALMUERZO")
                .with(user("prueba").roles("COCINA")))
            .andExpect(status().isOk()).andReturn();
        assertThat(resultado.getModelAndView().getModel().get("fechaConsulta")).isEqualTo(LocalDate.of(2026, 11, 8));
        Page<?> pagina = (Page<?>) resultado.getModelAndView().getModel().get("pagina");
        assertThat(pagina).hasSize(1);
        mvc.perform(get("/reportes/menus").param("fecha", "2026-11-08")
                .param("tiempoComida", "CENA").with(user("prueba").roles("COCINA")))
            .andExpect(status().isOk());
        var segunda = mvc.perform(get("/reportes/pacientes").param("pagina", "1")
                .with(user("prueba").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn();
        Page<?> fuera = (Page<?>) segunda.getModelAndView().getModel().get("pagina");
        assertThat(fuera.getNumber()).isEqualTo(1);
        assertThat(fuera.getTotalElements()).isEqualTo(1);
        assertThat(fuera).isEmpty();
    }

    @Test
    void cocinaRecibe403AntesDeEnlazarFiltrosOEjecutarConsultas() throws Exception {
        // El contador se obtiene de la unidad de persistencia real utilizada por la prueba.
        var estadisticas = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        em.flush(); em.clear();
        boolean habilitadas = estadisticas.isStatisticsEnabled();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            for (String reporte : List.of("pacientes", "ocupacion")) {
                mvc.perform(get("/reportes/" + reporte).param("pagina", "dato-invalido")
                        .with(user("prueba").roles("COCINA")))
                    .andExpect(status().isForbidden());
            }
            assertThat(estadisticas.getPrepareStatementCount()).isZero();
        } finally { estadisticas.setStatisticsEnabled(habilitadas); }
    }


    @Test
    void paginacionConservaFiltrosYAcentosYCaracteresEspeciales() throws Exception {
        String buscar = "A+B & Niño";
        for (int i = 0; i < 21; i++) {
            Paciente nuevo = new Paciente("UI-%02d".formatted(i), buscar, "Prueba", LocalDate.of(1990, 1, 1), new java.math.BigDecimal("65"));
            nuevo.setAlturaCm(new java.math.BigDecimal("170"));
            nuevo.setEstado(sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente.ALTA);
            nuevo.setCama(paciente.getCama());
            pacientes.save(nuevo);
        }
        pacientes.flush();
        String pisoId = paciente.getCama().getPiso().getId().toString();
        var primera = mvc.perform(get("/reportes/pacientes").param("buscar", buscar).param("estado", "ALTA")
                .param("pisoId", pisoId).with(user("prueba").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn();
        var enlaces = (sv.edu.ues.alimentacion_hospitalaria.reportes.web.EnlacesConsultaReportes.Enlaces)
            primera.getModelAndView().getModel().get("enlaces");
        assertThat(enlaces.siguiente()).contains("%2B", "%26", "%C3%B1", "pisoId=" + pisoId, "estado=ALTA", "pagina=1");
        assertThat(enlaces.anterior()).isNull();
        assertThat(primera.getResponse().getContentAsString()).contains("rel=\"next\"", "Referencia histórica", "A+B &amp; Niño")
            .doesNotContain("name=\"pagina\"");
        var segunda = mvc.perform(get(java.net.URI.create(enlaces.siguiente())).with(user("prueba").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn();
        var filtros = (FiltrosReporte.Pacientes) segunda.getModelAndView().getModel().get("filtros");
        assertThat(filtros.buscar()).isEqualTo(buscar);
        assertThat(filtros.pisoId()).isEqualTo(Long.valueOf(pisoId));
        assertThat(filtros.estado().name()).isEqualTo("ALTA");
        Page<?> pagina = (Page<?>) segunda.getModelAndView().getModel().get("pagina");
        assertThat(pagina).hasSize(1);
        assertThat(pagina.getTotalElements()).isEqualTo(21);
        assertThat(segunda.getResponse().getContentAsString()).contains("Página 2 de 2", "UI-20");
        var ultimoEnlace = (sv.edu.ues.alimentacion_hospitalaria.reportes.web.EnlacesConsultaReportes.Enlaces)
            segunda.getModelAndView().getModel().get("enlaces");
        assertThat(ultimoEnlace.siguiente()).isNull();
        assertThat(ultimoEnlace.anterior()).contains("pagina=0");
    }

    @Test
    void muestraCamasInconsistentesSinOmitirPacientesNiContarAltas() throws Exception {
        Paciente duplicado = new Paciente("DOBLE-02", "Ocupante", "Sintético", LocalDate.of(1990, 1, 1), new java.math.BigDecimal("65"));
        duplicado.setAlturaCm(new java.math.BigDecimal("170"));
        duplicado.setCama(paciente.getCama());
        pacientes.saveAndFlush(duplicado);
        var resultado = mvc.perform(get("/reportes/ocupacion").param("ocupacion", "INCONSISTENTE")
                .with(user("prueba").roles("ENCARGADO_PISO")))
            .andExpect(status().isOk()).andReturn();
        assertThat(resultado.getResponse().getContentAsString()).contains("Inconsistente", "Más de un paciente ingresado en esta cama.",
            "DOBLE-02", paciente.getCodigo());
    }

    @Test
    void formulariosMantienenSeleccionYEscapanTextoDeBusqueda() throws Exception {
        var resultado = mvc.perform(get("/reportes/menus").param("fecha", "2026-11-08")
                .param("tipoDieta", "BLANDA").param("tiempoComida", "ALMUERZO")
                .with(user("prueba").roles("COCINA")))
            .andExpect(status().isOk()).andReturn();
        String html = resultado.getResponse().getContentAsString();
        assertThat(html).contains("value=\"2026-11-08\"", "value=\"BLANDA\" selected=\"selected\"",
            "value=\"ALMUERZO\" selected=\"selected\"", "Aplicar filtros", "Limpiar filtros", "<td>12:00</td>");
        var vacio = mvc.perform(get("/reportes/pacientes").param("buscar", "<script>alert(1)</script>")
                .with(user("prueba").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn();
        assertThat(vacio.getResponse().getContentAsString()).contains("&lt;script&gt;alert(1)&lt;/script&gt;")
            .doesNotContain("<script>alert(1)</script>");
    }


    @ParameterizedTest
    @EnumSource(Rol.class)
    void impresionRespetaPermisosYEliminaNavegacionDeLaVista(Rol rol) throws Exception {
        for (String reporte : REPORTES) {
            boolean permitido = rol != Rol.ROLE_COCINA || reporte.equals("asignaciones") || reporte.equals("menus");
            for (String sufijo : List.of("", "/")) {
                String ruta = "/reportes/" + reporte + "/impresion" + sufijo;
                var respuesta = mvc.perform(get(ruta).param("fecha", "2026-11-08")
                        .with(user("prueba").roles(rol.name().substring(5))));
                if (permitido) {
                    var resultado = respuesta.andExpect(status().isOk()).andExpect(view().name("reportes/impresion")).andReturn();
                    String html = resultado.getResponse().getContentAsString();
                    assertThat(html).contains("Filtros aplicados", "Generado:", "Total de registros: 1", "<table")
                        .doesNotContain("app-sidebar", "Cerrar sesión", "Páginas del reporte", "name=\"pagina\"");
                    if (reporte.equals("asignaciones") && (rol == Rol.ROLE_COCINA || rol == Rol.ROLE_ENCARGADO_PISO)) {
                        assertThat(html).doesNotContain("Usuario responsable.http", "<th scope=\"col\">Responsable</th>",
                            "<th scope=\"col\">Inicio</th>", "<th scope=\"col\">Fin</th>");
                    }
                } else { respuesta.andExpect(status().isForbidden()); }
                mvc.perform(head(ruta).with(user("prueba").roles(rol.name().substring(5))))
                    .andExpect(permitido ? status().isOk() : status().isForbidden());
            }
            for (HttpMethod metodo : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
                mvc.perform(request(metodo, "/reportes/" + reporte + "/impresion")
                        .with(user("prueba").roles(rol.name().substring(5))).with(csrf()))
                    .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void imprimirConservaFiltrosEIncluyeTodoAunqueLaPaginaVisibleSeaOtra() throws Exception {
        var normal = mvc.perform(get("/reportes/asignaciones").param("buscar", paciente.getCodigo())
                .param("fecha", "2026-11-08").param("tipoDieta", "BLANDA").param("pagina", "99")
                .with(user("prueba").roles("COCINA")))
            .andExpect(status().isOk()).andReturn();
        var enlaces = (sv.edu.ues.alimentacion_hospitalaria.reportes.web.EnlacesConsultaReportes.Enlaces)
            normal.getModelAndView().getModel().get("enlaces");
        assertThat(enlaces.impresion()).contains("/asignaciones/impresion?", "buscar=" + paciente.getCodigo(),
            "fecha=2026-11-08", "tipoDieta=BLANDA").doesNotContain("pagina=");
        var impresion = mvc.perform(get(java.net.URI.create(enlaces.impresion())).with(user("prueba").roles("COCINA")))
            .andExpect(status().isOk()).andReturn();
        Page<?> pagina = (Page<?>) impresion.getModelAndView().getModel().get("pagina");
        assertThat(pagina).hasSize(1);
        assertThat(pagina.getNumber()).isZero();
        assertThat(impresion.getResponse().getContentAsString()).contains(paciente.getCodigo(), "Blanda", "08/11/2026");
    }

    @Test
    void impresionVaciaYFiltrosInvalidosTienenRespuestasDistintas() throws Exception {
        var vacio = mvc.perform(get("/reportes/menus/impresion").param("fecha", "2020-01-01")
                .with(user("prueba").roles("COCINA")))
            .andExpect(status().isOk()).andReturn();
        assertThat(vacio.getResponse().getContentAsString()).contains("No hay resultados para los filtros seleccionados.", "Total de registros: 0");
        mvc.perform(get("/reportes/menus/impresion").param("fecha", "2026-02-30")
                .with(user("prueba").roles("COCINA"))).andExpect(status().isBadRequest());
        mvc.perform(get("/reportes/pacientes/impresion")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void aceptaMilRegistrosYRechazaMilUnoAntesDeCargarFilas() throws Exception {
        var filas = new java.util.ArrayList<Object[]>();
        for (int i = 0; i < 1000; i++) {
            filas.add(new Object[]{"LIM-%04d".formatted(i)});
        }
        String sql = """
            INSERT INTO pacientes (codigo,nombres,apellidos,fecha_nacimiento,peso_kg,altura_cm,estado,
                created_at,created_by,updated_at,updated_by)
            VALUES (?, 'Limite', 'Sintético', '1990-01-01', 65, 170, 'ALTA', NOW(6), 'test', NOW(6), 'test')
            """;
        jdbc.batchUpdate(sql, filas);
        var aceptado = mvc.perform(get("/reportes/pacientes/impresion").param("buscar", "Limite")
                .with(user("prueba").roles("ADMIN")))
            .andExpect(status().isOk()).andReturn();
        assertThat((Page<?>) aceptado.getModelAndView().getModel().get("pagina")).hasSize(1000);
        assertThat(aceptado.getResponse().getContentAsString()).contains("LIM-0000", "LIM-0999", "Total de registros: 1000");
        jdbc.update(sql, "LIM-1000");
        em.flush(); em.clear();
        var estadisticas = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean habilitadas = estadisticas.isStatisticsEnabled();
        estadisticas.setStatisticsEnabled(true); estadisticas.clear();
        try {
            var rechazado = mvc.perform(get("/reportes/pacientes/impresion").param("buscar", "Limite")
                    .with(user("prueba").roles("ADMIN")))
                .andExpect(status().is(422)).andExpect(view().name("reportes/limite-impresion")).andReturn();
            assertThat(rechazado.getModelAndView().getModel()).doesNotContainKey("pagina");
            assertThat(rechazado.getResponse().getContentAsString()).contains("1001", "1000", "No se generó una impresión parcial.")
                .doesNotContain("LIM-0000", "LIM-1000");
            assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
        } finally { estadisticas.setStatisticsEnabled(habilitadas); }
    }

}
