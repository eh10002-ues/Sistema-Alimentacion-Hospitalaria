package sv.edu.ues.alimentacion_hospitalaria.asignaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.persistence.BitacoraRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;
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
class AsignacionesMenuWebTests {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/El_Salvador"));

    @Autowired private MockMvc mvc;
    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private MenuRepository menus;
    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;
    @Autowired private BitacoraRepository bitacora;

    private DatosClinicosPrueba clinicos;
    private Menu menuActivo;
    private String platillo;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        clinicos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        platillo = "Puré D4 " + UUID.randomUUID().toString().substring(0, 8);
        menuActivo = menu(EstadoMenu.ACTIVO);
        paciente = clinicos.pacienteIngresado();
    }

    @Test
    void asignacionAprobadaSeReflejaEnCocinaYReportes() throws Exception {
        Usuario medico = clinicos.usuario("d4.medico." + UUID.randomUUID(), Rol.ROLE_MEDICO);
        Usuario nutri = clinicos.usuario("d4.nutri." + UUID.randomUUID(), Rol.ROLE_NUTRICIONISTA);
        RequestPostProcessor cocina = sesion(clinicos.usuario("d4.cocina." + UUID.randomUUID(), Rol.ROLE_COCINA));
        String codigo = paciente.getCodigo();
        String sah6 = "/cocina/consultas/pacientes-por-menu?tipoDieta=" + menuActivo.getTipoDieta();

        mvc.perform(get("/asignaciones/nueva").with(sesion(medico))).andExpect(status().isOk())
            .andExpect(content().string(containsString(codigo)))
            .andExpect(content().string(containsString(menuActivo.getNombre())));
        mvc.perform(crear(medico, paciente.getId(), menuActivo.getId(), HOY, null))
            .andExpect(redirectedUrl("/asignaciones?pendientes=true"));
        AsignacionMenu asignacion = asignaciones.findByPacienteIdAndActivaTrue(paciente.getId()).getFirst();
        assertThat(asignacion.getAprobadoPor()).isNull();
        assertThat(asignacion.getAsignadoPor().getId()).isEqualTo(medico.getId());

        // Pendiente: aparece en RE-03 y RE-04; no cuenta para SAH-6 ni RE-02.
        mvc.perform(get("/asignaciones").param("pendientes", "true").with(sesion(nutri)))
            .andExpect(status().isOk()).andExpect(content().string(containsString(codigo)))
            .andExpect(content().string(containsString("Aprobar")));
        mvc.perform(get("/reportes/asignaciones-pendientes").with(sesion(nutri)))
            .andExpect(content().string(containsString(codigo)));
        mvc.perform(get("/reportes/pacientes-sin-menu").with(sesion(nutri)))
            .andExpect(content().string(containsString(codigo)));
        mvc.perform(get(sah6).with(cocina)).andExpect(content().string(not(containsString(codigo))));
        mvc.perform(get("/reportes/comandas").with(cocina)).andExpect(content().string(not(containsString(platillo))));

        mvc.perform(post("/asignaciones/{id}/aprobar", asignacion.getId()).with(sesion(nutri)).with(csrf()))
            .andExpect(redirectedUrl("/asignaciones?pendientes=true"))
            .andExpect(flash().attribute("mensaje", "Asignación aprobada."));
        assertThat(asignacion.getAprobadoPor().getId()).isEqualTo(nutri.getId());
        assertThat(asignacion.getFechaAprobacion()).isEqualTo(HOY);
        assertThat(bitacora.findAll()).anySatisfy(registro -> {
            assertThat(registro.getAccion()).isEqualTo("APROBAR");
            assertThat(registro.getIdAfectado()).isEqualTo(asignacion.getId());
        });

        // Aprobada: aparece en SAH-6 y RE-02; sale de RE-03 y RE-04.
        mvc.perform(get(sah6).with(cocina)).andExpect(content().string(containsString(codigo)));
        mvc.perform(get("/reportes/comandas").with(cocina)).andExpect(content().string(containsString(platillo)));
        mvc.perform(get("/reportes/asignaciones-pendientes").with(sesion(nutri)))
            .andExpect(content().string(not(containsString(codigo))));
        mvc.perform(get("/reportes/pacientes-sin-menu").with(sesion(nutri)))
            .andExpect(content().string(not(containsString(codigo))));

        // No se aprueba dos veces.
        mvc.perform(post("/asignaciones/{id}/aprobar", asignacion.getId()).with(sesion(nutri)).with(csrf()))
            .andExpect(flash().attribute("error", "La asignación ya fue aprobada."));
    }

    @Test
    void asignacionesInvalidasSeRechazanConMensaje() throws Exception {
        Usuario medico = clinicos.usuario("d4.medico." + UUID.randomUUID(), Rol.ROLE_MEDICO);
        Menu borrador = menu(EstadoMenu.BORRADOR);
        Paciente deAlta = clinicos.pacienteDeAlta();

        rechazada(medico, deAlta.getId(), menuActivo.getId(), HOY, null,
            "Solo se pueden asignar menús a pacientes ingresados.");
        rechazada(medico, paciente.getId(), borrador.getId(), HOY, null, "Solo se pueden asignar menús activos.");
        rechazada(medico, paciente.getId(), menuActivo.getId(), HOY.minusDays(1), null,
            "La fecha de inicio no puede ser anterior a hoy.");
        rechazada(medico, paciente.getId(), menuActivo.getId(), HOY.plusDays(3), HOY.plusDays(1),
            "La fecha de fin no puede ser anterior a la fecha de inicio.");
        mvc.perform(post("/asignaciones").with(sesion(medico)).with(csrf()).param("menuId", menuActivo.getId().toString()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Seleccione el paciente.")))
            .andExpect(content().string(containsString("Ingrese la fecha de inicio.")));
        assertThat(asignaciones.findByPacienteIdAndActivaTrue(paciente.getId())).isEmpty();

        // Solapamiento con otra asignación activa del mismo paciente.
        mvc.perform(crear(medico, paciente.getId(), menuActivo.getId(), HOY, HOY.plusDays(5)))
            .andExpect(status().is3xxRedirection());
        rechazada(medico, paciente.getId(), menuActivo.getId(), HOY.plusDays(5), null,
            "El paciente ya tiene una asignación de menú activa o pendiente que se cruza con esas fechas.");
        mvc.perform(crear(medico, paciente.getId(), menuActivo.getId(), HOY.plusDays(6), null))
            .andExpect(status().is3xxRedirection());
        assertThat(asignaciones.findByPacienteIdAndActivaTrue(paciente.getId())).hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ROLE_ADMIN", "ROLE_MEDICO", "ROLE_ENCARGADO_PISO", "ROLE_COCINA"})
    void soloNutricionistaAprueba(Rol rol) throws Exception {
        Usuario medico = clinicos.usuario("d4.medico." + UUID.randomUUID(), Rol.ROLE_MEDICO);
        AsignacionMenu pendiente = asignaciones.save(new AsignacionMenu(paciente, menuActivo, medico, HOY));
        Usuario usuario = clinicos.usuario("d4.otro." + UUID.randomUUID(), rol);
        mvc.perform(post("/asignaciones/{id}/aprobar", pendiente.getId()).with(sesion(usuario)).with(csrf()))
            .andExpect(status().isForbidden());
        assertThat(pendiente.getAprobadoPor()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ROLE_ENCARGADO_PISO", "ROLE_COCINA"})
    void encargadoDePisoYCocinaNoAccedenAAsignaciones(Rol rol) throws Exception {
        Usuario usuario = clinicos.usuario("d4.sin.acceso." + UUID.randomUUID(), rol);
        mvc.perform(get("/asignaciones").with(sesion(usuario))).andExpect(status().isForbidden());
        mvc.perform(get("/asignaciones/nueva").with(sesion(usuario))).andExpect(status().isForbidden());
        mvc.perform(crear(usuario, paciente.getId(), menuActivo.getId(), HOY, null)).andExpect(status().isForbidden());
        assertThat(asignaciones.findByPacienteIdAndActivaTrue(paciente.getId())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ROLE_ADMIN", "ROLE_NUTRICIONISTA"})
    void administracionYNutricionAsignanSinAprobar(Rol rol) throws Exception {
        Usuario usuario = clinicos.usuario("d4.asigna." + UUID.randomUUID(), rol);
        mvc.perform(get("/asignaciones").with(sesion(usuario))).andExpect(status().isOk())
            .andExpect(view().name("asignaciones/listado"));
        mvc.perform(crear(usuario, paciente.getId(), menuActivo.getId(), HOY, null)).andExpect(status().is3xxRedirection());
        assertThat(asignaciones.findByPacienteIdAndActivaTrue(paciente.getId()))
            .singleElement().satisfies(asignacion -> assertThat(asignacion.getAprobadoPor()).isNull());
    }

    @Test
    void altaSinCsrfSeRechaza() throws Exception {
        Usuario medico = clinicos.usuario("d4.medico." + UUID.randomUUID(), Rol.ROLE_MEDICO);
        mvc.perform(post("/asignaciones").with(sesion(medico))
                .param("pacienteId", paciente.getId().toString()).param("menuId", menuActivo.getId().toString())
                .param("fechaInicio", HOY.toString()))
            .andExpect(status().isForbidden());
        assertThat(asignaciones.findByPacienteIdAndActivaTrue(paciente.getId())).isEmpty();
    }

    private void rechazada(Usuario usuario, Long pacienteId, Long menuId, LocalDate inicio, LocalDate fin,
                           String mensaje) throws Exception {
        mvc.perform(crear(usuario, pacienteId, menuId, inicio, fin))
            .andExpect(status().isOk()).andExpect(view().name("asignaciones/formulario"))
            .andExpect(content().string(containsString(mensaje)));
    }

    private org.springframework.test.web.servlet.RequestBuilder crear(Usuario usuario, Long pacienteId, Long menuId,
                                                                     LocalDate inicio, LocalDate fin) {
        var solicitud = post("/asignaciones").with(sesion(usuario)).with(csrf())
            .param("pacienteId", pacienteId.toString()).param("menuId", menuId.toString())
            .param("fechaInicio", inicio.toString());
        return fin == null ? solicitud : solicitud.param("fechaFin", fin.toString());
    }

    private Menu menu(EstadoMenu estado) {
        Menu menu = new Menu("Menú D4 " + UUID.randomUUID().toString().substring(0, 8), TipoMenu.GENERAL,
            TipoDieta.PAPILLA);
        menu.setEstado(estado);
        menu.agregarDetalle(new MenuDetalle(TiempoComida.ALMUERZO, LocalTime.of(12, 0), platillo));
        return menus.saveAndFlush(menu);
    }

    private static RequestPostProcessor sesion(Usuario usuario) {
        return user(usuario.getNombreUsuario()).roles(usuario.getRol().name().substring("ROLE_".length()));
    }
}
