package sv.edu.ues.alimentacion_hospitalaria.cocina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.MenusPorPacienteVista;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.MenusPorPacienteVista.EstadoAsignacion;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.OpcionConsulta;
import sv.edu.ues.alimentacion_hospitalaria.cocina.dto.PacientesPorMenuVista;
import sv.edu.ues.alimentacion_hospitalaria.cocina.service.ConsultasCocinaService;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@WithMockUser(roles = "COCINA")
class ConsultasCocinaServiceTests {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 15);

    @Autowired private ConsultasCocinaService servicio;
    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private MenuRepository menus;
    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;

    private DatosClinicosPrueba clinicos;
    private DatosConsultasCocina datos;
    private Usuario nutricionista;

    @BeforeEach
    void preparar() {
        clinicos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        datos = new DatosConsultasCocina(clinicos, pacientes, menus, asignaciones);
        nutricionista = clinicos.usuario("nutri.consultas.servicio", Rol.ROLE_NUTRICIONISTA);
    }

    @Test
    void pacientesPorRegimenDevuelveDatosOperativosYElMenuDeCadaPaciente() {
        Menu menu = datos.menu("Blanda", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Paciente paciente = datos.pacienteIngresado("Ana", "Ramírez");
        datos.aprobada(paciente, menu, nutricionista, HOY.minusDays(1), HOY.plusDays(3));

        PacientesPorMenuVista vista = servicio.pacientesPorRegimen(TipoDieta.BLANDA, HOY);

        assertThat(vista.tipoDieta()).isEqualTo(TipoDieta.BLANDA);
        assertThat(vista.fecha()).isEqualTo(HOY);
        assertThat(vista.pacientes())
            .filteredOn(fila -> fila.codigo().equals(paciente.getCodigo()))
            .singleElement().satisfies(fila -> {
                assertThat(fila.nombreCompleto()).isEqualTo("Ana Ramírez");
                assertThat(fila.menu()).isEqualTo(menu.getNombre());
                assertThat(fila.piso()).startsWith(paciente.getCama().getPiso().getCodigo());
                assertThat(fila.cama()).isEqualTo(paciente.getCama().getNumero());
                assertThat(fila.fechaFin()).isEqualTo(HOY.plusDays(3));
            });
    }

    @Test
    void pacientesPorRegimenReuneVariosMenusYExcluyeMenusNoActivos() {
        Menu menuA = datos.menu("Hiposódica A", TipoDieta.HIPOSODICA, EstadoMenu.ACTIVO);
        Menu menuB = datos.menu("Hiposódica B", TipoDieta.HIPOSODICA, EstadoMenu.ACTIVO);
        Menu borrador = datos.menu("Hiposódica borrador", TipoDieta.HIPOSODICA, EstadoMenu.BORRADOR);
        Paciente pacienteA = datos.pacienteIngresado("Ana", "Uno");
        Paciente pacienteB = datos.pacienteIngresado("Beto", "Dos");
        Paciente pacienteBorrador = datos.pacienteIngresado("Ciro", "Tres");
        datos.aprobada(pacienteA, menuA, nutricionista, HOY, null);
        datos.aprobada(pacienteB, menuB, nutricionista, HOY, null);
        datos.aprobada(pacienteBorrador, borrador, nutricionista, HOY, null);

        assertThat(servicio.pacientesPorRegimen(TipoDieta.HIPOSODICA, HOY).pacientes())
            .extracting(PacientesPorMenuVista.Fila::codigo)
            .contains(pacienteA.getCodigo(), pacienteB.getCodigo())
            .doesNotContain(pacienteBorrador.getCodigo());
    }

    @Test
    void menusPorPacienteMuestraDietaActivaConHorariosEHistorialConEstados() {
        Paciente paciente = datos.pacienteIngresado("Luis", "Herrera");
        Menu anterior = datos.menu("Líquida", TipoDieta.LIQUIDA, EstadoMenu.ACTIVO);
        Menu actual = datos.menu("Hiposódica", TipoDieta.HIPOSODICA, EstadoMenu.ACTIVO);
        Menu siguiente = datos.menu("Normal", TipoDieta.NORMAL, EstadoMenu.ACTIVO);
        datos.aprobada(paciente, anterior, nutricionista, HOY.minusDays(10), HOY.minusDays(1));
        datos.aprobada(paciente, actual, nutricionista, HOY, null);
        datos.aprobada(paciente, siguiente, nutricionista, HOY.plusDays(5), null);
        datos.pendiente(paciente, siguiente, nutricionista, HOY.plusDays(8));

        MenusPorPacienteVista vista = servicio.menusPorPaciente(paciente.getId(), HOY);

        assertThat(vista.nombreCompleto()).isEqualTo("Luis Herrera");
        assertThat(vista.vigentes()).singleElement().satisfies(vigente -> {
            assertThat(vigente.menu()).isEqualTo(actual.getNombre());
            assertThat(vigente.tipoDieta()).isEqualTo("Hiposódica");
            assertThat(vigente.tiempos())
                .extracting(MenusPorPacienteVista.Tiempo::tiempoComida, MenusPorPacienteVista.Tiempo::horaServicio)
                .containsExactly(
                    tuple("Desayuno", LocalTime.of(7, 0)),
                    tuple("Almuerzo", LocalTime.of(12, 30)),
                    tuple("Cena", LocalTime.of(18, 0)));
        });
        assertThat(vista.historial())
            .extracting(MenusPorPacienteVista.Historial::estado)
            .containsExactly(EstadoAsignacion.PROGRAMADA, EstadoAsignacion.VIGENTE, EstadoAsignacion.FINALIZADA);
    }

    @Test
    void asignacionDesactivadaOConMenuInactivoNoSeConsideraDietaActiva() {
        Paciente paciente = datos.pacienteIngresado("Mia", "Soto");
        Menu inactivo = datos.menu("Retirado", TipoDieta.PAPILLA, EstadoMenu.INACTIVO);
        Menu desactivado = datos.menu("Suspendido", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        datos.aprobada(paciente, inactivo, nutricionista, HOY.minusDays(2), null);
        datos.aprobada(paciente, desactivado, nutricionista, HOY.minusDays(1), null).setActiva(false);

        MenusPorPacienteVista vista = servicio.menusPorPaciente(paciente.getId(), HOY);

        assertThat(vista.vigentes()).isEmpty();
        assertThat(vista.historial())
            .extracting(MenusPorPacienteVista.Historial::estado)
            .containsExactly(EstadoAsignacion.FINALIZADA, EstadoAsignacion.MENU_NO_DISPONIBLE);
    }

    @Test
    void soloOfreceYConsultaPacientesIngresados() {
        Paciente ingresado = datos.pacienteIngresado("Ana", "Interna");
        Paciente deAlta = clinicos.pacienteDeAlta();

        assertThat(servicio.pacientesInternos()).extracting(OpcionConsulta::id)
            .contains(ingresado.getId())
            .doesNotContain(deAlta.getId());
        assertThatThrownBy(() -> servicio.menusPorPaciente(deAlta.getId(), HOY))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void elServicioTambienRechazaAlMedico() {
        assertThatThrownBy(() -> servicio.pacientesPorRegimen(TipoDieta.NORMAL, HOY))
            .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_PISO")
    void elServicioTambienRechazaAlEncargadoDePiso() {
        assertThatThrownBy(() -> servicio.pacientesInternos())
            .isInstanceOf(AuthorizationDeniedException.class);
    }
}
