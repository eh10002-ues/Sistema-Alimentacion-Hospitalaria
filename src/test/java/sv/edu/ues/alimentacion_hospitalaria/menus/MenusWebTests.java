package sv.edu.ues.alimentacion_hospitalaria.menus;

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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;
import sv.edu.ues.alimentacion_hospitalaria.insumos.persistence.InsumoRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class MenusWebTests {

    private static final Set<Rol> ADMINISTRAN = EnumSet.of(Rol.ROLE_ADMIN, Rol.ROLE_NUTRICIONISTA);

    @Autowired private MockMvc mvc;
    @Autowired private MenuRepository menus;
    @Autowired private InsumoRepository insumos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private JdbcTemplate jdbc;

    private Menu existente;

    @BeforeEach
    void preparar() {
        existente = new Menu("Menú existente " + UUID.randomUUID(), TipoMenu.GENERAL, TipoDieta.BLANDA);
        existente.agregarDetalle(new MenuDetalle(TiempoComida.ALMUERZO, LocalTime.of(12, 0), "Arroz con pollo"));
        existente = menus.saveAndFlush(existente);
    }

    @Test
    void nutricionistaAdministraMenuYSusTiemposDeComida() throws Exception {
        RequestPostProcessor nutri = usuario(Rol.ROLE_NUTRICIONISTA);
        String nombre = "Menú hiposódico " + UUID.randomUUID();
        mvc.perform(get("/menus/nuevo").with(nutri)).andExpect(status().isOk()).andExpect(view().name("menus/formulario"));

        var creado = mvc.perform(post("/menus").with(nutri).with(csrf())
                .param("nombre", nombre).param("tipo", "GENERAL").param("tipoDieta", "HIPOSODICA"))
            .andExpect(status().is3xxRedirection()).andReturn();
        Menu menu = menus.findAll().stream().filter(m -> m.getNombre().equals(nombre)).findFirst().orElseThrow();
        assertThat(creado.getResponse().getRedirectedUrl()).isEqualTo("/menus/" + menu.getId() + "/detalles");
        assertThat(menu.getEstado()).isEqualTo(EstadoMenu.BORRADOR);
        String base = "/menus/" + menu.getId();

        // No se activa sin tiempos de comida.
        mvc.perform(post(base + "/activar").with(nutri).with(csrf()))
            .andExpect(flash().attribute("error", "Agregue al menos un tiempo de comida antes de activar el menú."));
        assertThat(menu.getEstado()).isEqualTo(EstadoMenu.BORRADOR);

        mvc.perform(get(base + "/editar/detalles/nuevo").with(nutri)).andExpect(status().isOk());
        mvc.perform(post(base + "/editar/detalles").with(nutri).with(csrf())
                .param("tiempoComida", "DESAYUNO").param("horaServicio", "07:30").param("platillo", "Avena sin sal"))
            .andExpect(redirectedUrl(base + "/detalles"));
        // Un tiempo de comida no se repite en el mismo menú.
        mvc.perform(post(base + "/editar/detalles").with(nutri).with(csrf())
                .param("tiempoComida", "DESAYUNO").param("horaServicio", "08:00").param("platillo", "Otro"))
            .andExpect(status().isOk()).andExpect(view().name("menus/detalle-formulario"))
            .andExpect(content().string(containsString("El menú ya tiene registrado el tiempo de comida Desayuno.")));
        menus.flush();
        assertThat(menu.getDetalles()).hasSize(1);
        Long detalleId = menu.getDetalles().getFirst().getId();
        String detalle = base + "/editar/detalles/" + detalleId;

        mvc.perform(get(detalle).with(nutri)).andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"07:30\"")));
        mvc.perform(post(detalle).with(nutri).with(csrf())
                .param("tiempoComida", "DESAYUNO").param("horaServicio", "07:00").param("platillo", "Avena con manzana"))
            .andExpect(redirectedUrl(base + "/detalles"));
        assertThat(menu.getDetalles().getFirst().getPlatillo()).isEqualTo("Avena con manzana");
        assertThat(menu.getDetalles().getFirst().getHoraServicio()).isEqualTo(LocalTime.of(7, 0));

        Insumo avena = insumos.save(new Insumo("Avena " + UUID.randomUUID(), "g"));
        mvc.perform(post(detalle + "/insumos").with(nutri).with(csrf())
                .param("insumoId", avena.getId().toString()).param("cantidadPorRacion", "40"))
            .andExpect(redirectedUrl(detalle));
        mvc.perform(post(detalle + "/insumos").with(nutri).with(csrf())
                .param("insumoId", avena.getId().toString()).param("cantidadPorRacion", "45.5"))
            .andExpect(redirectedUrl(detalle));
        menus.flush();
        var insumosDetalle = menu.getDetalles().getFirst().getInsumos();
        assertThat(insumosDetalle).singleElement()
            .satisfies(item -> assertThat(item.getCantidadPorRacion()).isEqualByComparingTo(new BigDecimal("45.50")));
        mvc.perform(post(detalle + "/insumos").with(nutri).with(csrf())
                .param("insumoId", avena.getId().toString()).param("cantidadPorRacion", "0"))
            .andExpect(flash().attribute("error", "La cantidad por ración debe ser mayor que cero."));
        mvc.perform(post(detalle + "/insumos/" + insumosDetalle.getFirst().getId() + "/eliminar").with(nutri).with(csrf()))
            .andExpect(redirectedUrl(detalle));
        assertThat(menu.getDetalles().getFirst().getInsumos()).isEmpty();

        mvc.perform(post(base + "/activar").with(nutri).with(csrf())).andExpect(redirectedUrl(base + "/detalles"));
        assertThat(menu.getEstado()).isEqualTo(EstadoMenu.ACTIVO);
        // Un menú activo conserva al menos un tiempo de comida.
        mvc.perform(post(detalle + "/eliminar").with(nutri).with(csrf()))
            .andExpect(flash().attribute("error",
                "Un menú activo debe conservar al menos un tiempo de comida. Desactívelo antes de quitarlo."));

        mvc.perform(get(base + "/editar").with(nutri)).andExpect(status().isOk());
        mvc.perform(post(base + "/editar").with(nutri).with(csrf())
                .param("nombre", nombre + " editado").param("tipo", "PERSONALIZADO").param("tipoDieta", "BLANDA"))
            .andExpect(redirectedUrl(base + "/detalles"));
        assertThat(menu.getTipoDieta()).isEqualTo(TipoDieta.BLANDA);
        assertThat(menu.getTipo()).isEqualTo(TipoMenu.PERSONALIZADO);

        mvc.perform(post(base + "/desactivar").with(nutri).with(csrf())).andExpect(redirectedUrl(base + "/detalles"));
        assertThat(menu.getEstado()).isEqualTo(EstadoMenu.INACTIVO);

        mvc.perform(get(base + "/detalles").with(nutri)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Avena con manzana")))
            .andExpect(content().string(containsString("07:00")));
        Integer registros = jdbc.queryForObject(
            "select count(*) from bitacora where tabla_afectada in ('menus', 'menu_detalles') and id_afectado in (?, ?)",
            Integer.class, menu.getId(), detalleId);
        assertThat(registros).isGreaterThanOrEqualTo(8);
    }

    @Test
    void formularioInvalidoNoCreaMenu() throws Exception {
        long antes = menus.count();
        mvc.perform(post("/menus").with(usuario(Rol.ROLE_ADMIN)).with(csrf())
                .param("nombre", " ").param("tipo", "GENERAL"))
            .andExpect(status().isOk()).andExpect(view().name("menus/formulario"))
            .andExpect(content().string(containsString("Ingrese el nombre del menú.")))
            .andExpect(content().string(containsString("Seleccione el tipo de dieta.")));
        assertThat(menus.count()).isEqualTo(antes);
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void consultaParaTodosYModificacionSoloAdministracionYNutricion(Rol rol) throws Exception {
        RequestPostProcessor quien = usuario(rol);
        boolean administra = ADMINISTRAN.contains(rol);
        String base = "/menus/" + existente.getId();
        mvc.perform(get("/menus").with(quien)).andExpect(status().isOk())
            .andExpect(content().string(containsString(existente.getNombre())));
        mvc.perform(get(base + "/detalles").with(quien)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Arroz con pollo")))
            .andExpect(content().string(administra ? containsString("Editar datos") : not(containsString("Editar datos"))));
        var esperado = administra ? status().isOk() : status().isForbidden();
        mvc.perform(get("/menus/nuevo").with(quien)).andExpect(esperado);
        mvc.perform(get(base + "/editar").with(quien)).andExpect(esperado);
        mvc.perform(get(base + "/editar/detalles/nuevo").with(quien)).andExpect(esperado);
        mvc.perform(post(base + "/desactivar").with(quien).with(csrf()))
            .andExpect(administra ? status().is3xxRedirection() : status().isForbidden());
        assertThat(existente.getEstado()).isEqualTo(administra ? EstadoMenu.INACTIVO : EstadoMenu.BORRADOR);
    }

    @Test
    void modificacionSinCsrfSeRechaza() throws Exception {
        mvc.perform(post("/menus/" + existente.getId() + "/activar").with(usuario(Rol.ROLE_NUTRICIONISTA)))
            .andExpect(status().isForbidden());
        assertThat(existente.getEstado()).isEqualTo(EstadoMenu.BORRADOR);
    }

    private RequestPostProcessor usuario(Rol rol) {
        String nombre = "d4." + rol.name().toLowerCase() + "." + UUID.randomUUID().toString().substring(0, 8);
        usuarios.save(new Usuario(nombre, "{noop}no-se-usa", "Usuario " + nombre, rol, true));
        return user(nombre).roles(rol.name().substring("ROLE_".length()));
    }
}
