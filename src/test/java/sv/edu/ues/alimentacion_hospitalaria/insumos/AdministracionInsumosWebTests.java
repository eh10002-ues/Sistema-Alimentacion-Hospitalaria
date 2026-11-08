package sv.edu.ues.alimentacion_hospitalaria.insumos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.persistence.BitacoraRepository;
import sv.edu.ues.alimentacion_hospitalaria.insumos.domain.Insumo;
import sv.edu.ues.alimentacion_hospitalaria.insumos.persistence.InsumoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AdministracionInsumosWebTests {

    @Autowired private MockMvc mvc;
    @Autowired private InsumoRepository insumos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private BitacoraRepository bitacora;
    @Autowired private JdbcTemplate jdbc;

    private Usuario nutricionista;

    @BeforeEach
    void preparar() {
        nutricionista = usuarios.save(new Usuario(
            "nutri." + UUID.randomUUID(), "hash", "Nutricionista de prueba", Rol.ROLE_NUTRICIONISTA, true));
    }

    @Test
    void nutricionistaRegistraEditaYEliminaInsumoNoAsociado() throws Exception {
        mvc.perform(get("/insumos").with(usuario(nutricionista)))
            .andExpect(status().isOk())
            .andExpect(view().name("insumos/listado"))
            .andExpect(content().string(containsString("Catálogo de insumos")));
        mvc.perform(get("/insumos/nuevo").with(usuario(nutricionista)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("list=\"unidades-insumo\"")))
            .andExpect(content().string(containsString("<option value=\"lb\">Libra</option>")));

        mvc.perform(post("/insumos").with(usuario(nutricionista))
                .param("nombre", "Arroz").param("unidad", "g")
                .param("mermaPorcentaje", "10").param("presentacion", "Bolsa de 1 kg")
                .param("cantidadPresentacion", "1000").param("costoUnitario", "0.0010"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/insumos").with(usuario(nutricionista)).with(csrf())
                .param("nombre", "Arroz").param("unidad", "g")
                .param("mermaPorcentaje", "10").param("presentacion", "Bolsa de 1 kg")
                .param("cantidadPresentacion", "1000").param("costoUnitario", "0.0010"))
            .andExpect(redirectedUrl("/insumos"))
            .andExpect(flash().attribute("mensaje", "Insumo registrado correctamente."));

        Insumo arroz = insumos.findByNombreIgnoreCase("arroz").orElseThrow();
        assertThat(arroz.getMerma()).isEqualByComparingTo("0.1000");
        assertThat(arroz.getCantidadPresentacion()).isEqualByComparingTo("1000");

        mvc.perform(post("/insumos/{id}", arroz.getId()).with(usuario(nutricionista)).with(csrf())
                .param("nombre", "Arroz integral").param("unidad", "g")
                .param("mermaPorcentaje", "5").param("costoUnitario", "0.0020"))
            .andExpect(redirectedUrl("/insumos"))
            .andExpect(flash().attribute("mensaje", "Insumo actualizado correctamente."));
        assertThat(insumos.findById(arroz.getId()).orElseThrow().getNombre()).isEqualTo("Arroz integral");

        mvc.perform(post("/insumos/{id}/eliminar", arroz.getId()).with(usuario(nutricionista)).with(csrf()))
            .andExpect(redirectedUrl("/insumos"))
            .andExpect(flash().attribute("mensaje", "Insumo eliminado correctamente."));
        assertThat(insumos.existsById(arroz.getId())).isFalse();
        assertThat(bitacora.findByTablaAfectadaAndIdAfectadoOrderByFechaAccionDesc("insumos", arroz.getId()))
            .extracting(registro -> registro.getAccion())
            .contains("CREAR", "ACTUALIZAR", "ELIMINAR");
    }

    @Test
    void noPermiteNombresDuplicadosSinDistinguirMayusculas() throws Exception {
        insumos.save(new Insumo("Arroz", "g"));

        mvc.perform(post("/insumos").with(usuario(nutricionista)).with(csrf())
                .param("nombre", "ARROZ").param("unidad", "g").param("mermaPorcentaje", "0"))
            .andExpect(status().isOk())
            .andExpect(view().name("insumos/formulario"))
            .andExpect(content().string(containsString("Ya existe un insumo con ese nombre.")));

        assertThat(insumos.count()).isEqualTo(1);
    }

    @Test
    void cocinaPuedeConsultarLaProyeccionPeroNoElCatalogo() throws Exception {
        Usuario cocina = usuarios.save(new Usuario(
            "cocina." + UUID.randomUUID(), "hash", "Cocina de prueba", Rol.ROLE_COCINA, true));

        mvc.perform(get("/insumos").with(user(cocina.getNombreUsuario()).roles("COCINA")))
            .andExpect(status().isForbidden());

        mvc.perform(get("/insumos/proyeccion").with(user(cocina.getNombreUsuario()).roles("COCINA")))
            .andExpect(status().isOk())
            .andExpect(view().name("insumos/proyeccion"));
    }

    @Test
    void impideEliminarInsumoAsociadoAMenu() throws Exception {
        Insumo arroz = insumos.save(new Insumo("Arroz usado", "g"));
        String sufijo = UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO menus
                (nombre, tipo, tipo_dieta, estado, created_at, created_by, updated_at, updated_by)
            VALUES (?, 'GENERAL', 'NORMAL', 'ACTIVO', NOW(6), 'test', NOW(6), 'test')
            """, "Menú de prueba " + sufijo);
        Long menuId = jdbc.queryForObject("SELECT id FROM menus WHERE nombre = ?", Long.class, "Menú de prueba " + sufijo);
        jdbc.update("""
            INSERT INTO menu_detalles (menu_id, tiempo_comida, hora_servicio, platillo)
            VALUES (?, 'ALMUERZO', '12:00:00', 'Arroz')
            """, menuId);
        Long detalleId = jdbc.queryForObject(
            "SELECT id FROM menu_detalles WHERE menu_id = ?", Long.class, menuId);
        jdbc.update("""
            INSERT INTO menu_detalle_insumos (menu_detalle_id, insumo_id, cantidad_por_racion)
            VALUES (?, ?, 80.00)
            """, detalleId, arroz.getId());

        mvc.perform(get("/insumos").with(usuario(nutricionista)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Asociado a un menú; no se puede eliminar.")));

        mvc.perform(post("/insumos/{id}/eliminar", arroz.getId()).with(usuario(nutricionista)).with(csrf()))
            .andExpect(redirectedUrl("/insumos"))
            .andExpect(flash().attribute("error",
                "No se puede eliminar el insumo porque está asociado a uno o más menús."));
        assertThat(insumos.existsById(arroz.getId())).isTrue();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor usuario(Usuario usuario) {
        return user(usuario.getNombreUsuario()).roles("NUTRICIONISTA");
    }
}
