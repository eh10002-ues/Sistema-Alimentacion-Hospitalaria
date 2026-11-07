package sv.edu.ues.alimentacion_hospitalaria.insumos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.persistence.BitacoraRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ProyeccionInsumosWebTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void cocinaPuedeVerLaProyeccionConDatosRealesYQuedaAuditada() throws Exception {
        usuarioRepository.save(new Usuario("cocina", "hash", "Cocina", Rol.ROLE_COCINA, true));
        Usuario nutricionista = usuarioRepository.save(
            new Usuario("nutri.proyeccion", "hash", "Nutricionista de prueba", Rol.ROLE_NUTRICIONISTA, true));
        insertarDatosProyeccion(nutricionista.getId());

        String html = mvc.perform(get("/insumos/proyeccion").with(user("cocina").roles("COCINA")))
            .andExpect(status().isOk())
            .andExpect(view().name("insumos/proyeccion"))
            .andReturn().getResponse().getContentAsString();
        assertThat(html)
            .contains("Arroz de prueba", "data-raw=\"560.00\"", "data-unidad=\"g\"")
            .contains("Pacientes incluidos", "Raciones por día (promedio)");

        assertThat(bitacoraRepository.findByTablaAfectadaAndIdAfectadoOrderByFechaAccionDesc(
            "insumos", null))
            .hasSize(1)
            .first()
            .satisfies(registro -> {
                assertThat(registro.getAccion()).isEqualTo("PROYECTAR");
                assertThat(registro.getUsuario().getNombreUsuario()).isEqualTo("cocina");
            });
    }

    @Test
    void medicoNoPuedeConsultarLaProyeccion() throws Exception {
        mvc.perform(get("/insumos/proyeccion").with(user("medico").roles("MEDICO")))
            .andExpect(status().isForbidden())
            .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void periodoMensualComienzaEnFechaDeElSalvadorYTermina29DiasDespues() throws Exception {
        usuarioRepository.save(new Usuario("cocina", "hash", "Cocina", Rol.ROLE_COCINA, true));
        LocalDate hoyEnElSalvador = LocalDate.now(ZoneId.of("America/El_Salvador"));

        mvc.perform(get("/insumos/proyeccion")
                .param("periodo", "MENSUAL")
                .param("margenFraccion", "0.10")
                .with(user("cocina").roles("COCINA")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                hoyEnElSalvador.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))))
            .andExpect(content().string(containsString(
                hoyEnElSalvador.plusDays(29).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))))
            .andExpect(content().string(containsString("Margen <span>10.00</span>%")));
    }

    private void insertarDatosProyeccion(Long usuarioId) {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/El_Salvador"));
        jdbc.update("""
            INSERT INTO insumos
                (nombre, unidad, merma, presentacion, cantidad_presentacion, costo_unitario)
            VALUES (?, 'g', 0.1000, 'paquete 1 kg', 1000.00, 0.0010)
            """, "Arroz de prueba");
        Long insumoId = jdbc.queryForObject(
            "SELECT id FROM insumos WHERE nombre = ?", Long.class, "Arroz de prueba");

        jdbc.update("""
            INSERT INTO menus
                (nombre, tipo, tipo_dieta, estado, created_at, created_by, updated_at, updated_by)
            VALUES ('Menú de prueba', 'GENERAL', 'NORMAL', 'ACTIVO', NOW(6), 'test', NOW(6), 'test')
            """);
        Long menuId = jdbc.queryForObject(
            "SELECT id FROM menus WHERE nombre = 'Menú de prueba'", Long.class);
        jdbc.update("""
            INSERT INTO menu_detalles (menu_id, tiempo_comida, hora_servicio, platillo)
            VALUES (?, 'ALMUERZO', '12:00:00', 'Arroz de prueba')
            """, menuId);
        Long detalleId = jdbc.queryForObject(
            "SELECT id FROM menu_detalles WHERE menu_id = ?", Long.class, menuId);
        jdbc.update("""
            INSERT INTO menu_detalle_insumos (menu_detalle_id, insumo_id, cantidad_por_racion)
            VALUES (?, ?, 80.00)
            """, detalleId, insumoId);

        jdbc.update("""
            INSERT INTO pacientes
                (codigo, nombres, apellidos, fecha_nacimiento, peso_kg, altura_cm, estado,
                 cama_id, medico_responsable_id, created_at, created_by, updated_at, updated_by)
            VALUES ('PROY-TEST-1', 'Paciente', 'Prueba', '1980-01-01', 70.00, 170.00, 'INGRESADO',
                    NULL, ?, NOW(6), 'test', NOW(6), 'test')
            """, usuarioId);
        Long pacienteId = jdbc.queryForObject(
            "SELECT id FROM pacientes WHERE codigo = 'PROY-TEST-1'", Long.class);

        jdbc.update("""
            INSERT INTO asignaciones_menu
                (paciente_id, menu_id, asignado_por_id, fecha_inicio, fecha_fin, activa,
                 aprobado_por_id, fecha_aprobacion, created_at, created_by, updated_at, updated_by)
            VALUES (?, ?, ?, ?, NULL, TRUE, ?, ?, NOW(6), 'test', NOW(6), 'test')
            """, pacienteId, menuId, usuarioId, java.sql.Date.valueOf(hoy.minusDays(1)),
            usuarioId, java.sql.Date.valueOf(hoy));
    }
}
