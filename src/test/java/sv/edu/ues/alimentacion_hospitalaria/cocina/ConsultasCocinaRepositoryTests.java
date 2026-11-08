package sv.edu.ues.alimentacion_hospitalaria.cocina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
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

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ConsultasCocinaRepositoryTests {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 15);

    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private MenuRepository menus;
    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;

    private DatosConsultasCocina datos;
    private Usuario nutricionista;

    @BeforeEach
    void preparar() {
        DatosClinicosPrueba clinicos = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes);
        datos = new DatosConsultasCocina(clinicos, pacientes, menus, asignaciones);
        nutricionista = clinicos.usuario("nutri.consultas.repo", Rol.ROLE_NUTRICIONISTA);
    }

    @Test
    void pacientesPorRegimenIncluyeTodosLosMenusActivosDelMismoTipoDieta() {
        Menu menu = datos.menu("Blanda A", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Menu otroMenuMismoRegimen = datos.menu("Blanda B", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Menu menuInactivo = datos.menu("Blanda retirada", TipoDieta.BLANDA, EstadoMenu.INACTIVO);
        Menu otroRegimen = datos.menu("Normal", TipoDieta.NORMAL, EstadoMenu.ACTIVO);

        Paciente enMenuA = datos.pacienteIngresado("Ana", "MenuA");
        Paciente enMenuB = datos.pacienteIngresado("Beto", "MenuB");
        Paciente enInactivo = datos.pacienteIngresado("Ciro", "MenuInactivo");
        Paciente enOtroRegimen = datos.pacienteIngresado("Dina", "OtroRegimen");
        datos.aprobada(enMenuA, menu, nutricionista, HOY.minusDays(1), null);
        datos.aprobada(enMenuB, otroMenuMismoRegimen, nutricionista, HOY.minusDays(1), null);
        datos.aprobada(enInactivo, menuInactivo, nutricionista, HOY.minusDays(1), null);
        datos.aprobada(enOtroRegimen, otroRegimen, nutricionista, HOY.minusDays(1), null);

        assertThat(asignaciones.findVigentesPorTipoDieta(TipoDieta.BLANDA, EstadoMenu.ACTIVO,
                EstadoPaciente.INGRESADO, HOY))
            .extracting(asignacion -> asignacion.getPaciente().getApellidos(), asignacion -> asignacion.getMenu().getNombre())
            .containsExactlyInAnyOrder(
                tuple("MenuA", menu.getNombre()),
                tuple("MenuB", otroMenuMismoRegimen.getNombre()));
    }

    @Test
    void pacientesPorRegimenSoloIncluyeInternosConAsignacionActivaAprobadaYVigente() {
        Menu menu = datos.menu("Blanda", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Menu otroMenu = datos.menu("Normal", TipoDieta.NORMAL, EstadoMenu.ACTIVO);

        Paciente vigente = datos.pacienteIngresado("Ana", "Vigente");
        Paciente sinFin = datos.pacienteIngresado("Beto", "SinFin");
        Paciente pendiente = datos.pacienteIngresado("Carla", "Pendiente");
        Paciente inactiva = datos.pacienteIngresado("Dora", "Inactiva");
        Paciente vencida = datos.pacienteIngresado("Eva", "Vencida");
        Paciente futura = datos.pacienteIngresado("Fito", "Futura");
        Paciente deAlta = datos.pacienteIngresado("Gina", "Alta");
        Paciente otro = datos.pacienteIngresado("Hugo", "OtroMenu");

        datos.aprobada(vigente, menu, nutricionista, HOY.minusDays(2), HOY);
        datos.aprobada(sinFin, menu, nutricionista, HOY, null);
        datos.pendiente(pendiente, menu, nutricionista, HOY.minusDays(1));
        AsignacionMenu desactivada = datos.aprobada(inactiva, menu, nutricionista, HOY.minusDays(1), null);
        desactivada.setActiva(false);
        datos.aprobada(vencida, menu, nutricionista, HOY.minusDays(5), HOY.minusDays(1));
        datos.aprobada(futura, menu, nutricionista, HOY.plusDays(1), null);
        datos.aprobada(deAlta, menu, nutricionista, HOY.minusDays(1), null);
        deAlta.setEstado(EstadoPaciente.ALTA);
        datos.aprobada(otro, otroMenu, nutricionista, HOY.minusDays(1), null);

        assertThat(asignaciones.findVigentesPorTipoDieta(TipoDieta.BLANDA, EstadoMenu.ACTIVO,
                EstadoPaciente.INGRESADO, HOY))
            .extracting(asignacion -> asignacion.getPaciente().getApellidos())
            .containsExactlyInAnyOrder("Vigente", "SinFin");
    }

    @Test
    void historialSoloIncluyeAsignacionesAprobadasDeLaMasRecienteALaMasAntigua() {
        Paciente paciente = datos.pacienteIngresado("Ana", "Historial");
        Menu menu = datos.menu("Hiposódica", TipoDieta.HIPOSODICA, EstadoMenu.ACTIVO);

        datos.aprobada(paciente, menu, nutricionista, HOY.minusDays(20), HOY.minusDays(11));
        datos.aprobada(paciente, menu, nutricionista, HOY.minusDays(10), null);
        datos.pendiente(paciente, menu, nutricionista, HOY.plusDays(3));

        assertThat(asignaciones.findHistorialAprobadoPorPaciente(paciente.getId()))
            .extracting(AsignacionMenu::getFechaInicio)
            .containsExactly(HOY.minusDays(10), HOY.minusDays(20));
    }
}
