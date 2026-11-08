package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.List;
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
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilaComanda;
import sv.edu.ues.alimentacion_hospitalaria.reportes.persistence.ReportesRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ReportesRepositoryTests {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 15);

    @Autowired private ReportesRepository reportes;
    @Autowired private AsignacionMenuRepository asignaciones;
    @Autowired private MenuRepository menus;
    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PisoRepository pisos;
    @Autowired private CamaRepository camas;

    private DatosReportes datos;
    private Usuario nutricionista;

    @BeforeEach
    void preparar() {
        datos = new DatosReportes(pisos, camas, pacientes, menus, asignaciones);
        nutricionista = new DatosClinicosPrueba(usuarios, pisos, camas, pacientes)
            .usuario("nutri.reportes.repo", Rol.ROLE_NUTRICIONISTA);
    }

    @Test
    void alergiasSoloIncluyeSeveridadSeveraDePacientesIngresadosYFiltraPorPiso() {
        Piso pisoA = datos.piso("Piso A");
        Piso pisoB = datos.piso("Piso B");
        Paciente ana = datos.paciente(pisoA, "Ana", "Alergias");
        datos.conAlergia(ana, "Maní", SeveridadAlergia.SEVERA);
        datos.conAlergia(ana, "Polvo", SeveridadAlergia.LEVE);
        datos.conAlergia(ana, "Látex", SeveridadAlergia.MODERADA);
        Paciente deAlta = datos.conAlergia(datos.paciente(pisoA, "Beto", "Alta"), "Huevo", SeveridadAlergia.SEVERA);
        deAlta.setEstado(EstadoPaciente.ALTA);
        Paciente otroPiso = datos.conAlergia(datos.paciente(pisoB, "Ciro", "OtroPiso"), "Leche", SeveridadAlergia.SEVERA);

        assertThat(reportes.buscarAlergias(SeveridadAlergia.SEVERA, EstadoPaciente.INGRESADO, pisoA.getId()))
            .extracting(alergia -> alergia.getPaciente().getApellidos(), PacienteAlergia::getNombre)
            .containsExactly(tuple("Alergias", "Maní"));
        assertThat(reportes.buscarAlergias(SeveridadAlergia.SEVERA, EstadoPaciente.INGRESADO, null))
            .extracting(alergia -> alergia.getPaciente().getId())
            .contains(ana.getId(), otroPiso.getId())
            .doesNotContain(deAlta.getId());
    }

    @Test
    void racionesSoloCuentanAsignacionesVigentesYSeAgrupanPorTiempoMenuYPiso() {
        Piso piso = datos.piso("Piso comandas");
        Menu blanda = datos.menu("Blanda", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Menu normal = datos.menu("Normal", TipoDieta.NORMAL, EstadoMenu.ACTIVO);
        Menu borrador = datos.menu("Borrador", TipoDieta.BLANDA, EstadoMenu.BORRADOR);
        datos.aprobada(datos.paciente(piso, "Ana", "Uno"), blanda, nutricionista, HOY.minusDays(1), null);
        datos.aprobada(datos.paciente(piso, "Beto", "Dos"), blanda, nutricionista, HOY, HOY);
        datos.aprobada(datos.paciente(piso, "Ciro", "Tres"), normal, nutricionista, HOY.minusDays(3), HOY.plusDays(3));
        // Casos que no deben contarse.
        datos.pendiente(datos.paciente(piso, "Dora", "Pendiente"), blanda, nutricionista, HOY);
        datos.aprobada(datos.paciente(piso, "Eva", "Inactiva"), blanda, nutricionista, HOY, null).setActiva(false);
        datos.aprobada(datos.paciente(piso, "Fito", "Vencida"), blanda, nutricionista, HOY.minusDays(5), HOY.minusDays(1));
        datos.aprobada(datos.paciente(piso, "Gina", "Futura"), blanda, nutricionista, HOY.plusDays(1), null);
        datos.aprobada(datos.paciente(piso, "Hugo", "Borrador"), borrador, nutricionista, HOY, null);
        Paciente deAlta = datos.paciente(piso, "Iris", "Alta");
        datos.aprobada(deAlta, blanda, nutricionista, HOY, null);
        deAlta.setEstado(EstadoPaciente.ALTA);

        List<FilaComanda> filas = reportes.contarRaciones(EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO, HOY,
            piso.getId(), null);

        assertThat(filas)
            .extracting(FilaComanda::tiempoComida, FilaComanda::menu, FilaComanda::pisoCodigo, FilaComanda::raciones)
            .containsExactlyInAnyOrder(
                tuple(TiempoComida.DESAYUNO, blanda.getNombre(), piso.getCodigo(), 2L),
                tuple(TiempoComida.ALMUERZO, blanda.getNombre(), piso.getCodigo(), 2L),
                tuple(TiempoComida.CENA, blanda.getNombre(), piso.getCodigo(), 2L),
                tuple(TiempoComida.DESAYUNO, normal.getNombre(), piso.getCodigo(), 1L),
                tuple(TiempoComida.ALMUERZO, normal.getNombre(), piso.getCodigo(), 1L),
                tuple(TiempoComida.CENA, normal.getNombre(), piso.getCodigo(), 1L));
        assertThat(reportes.contarRaciones(EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO, HOY, piso.getId(),
                TiempoComida.DESAYUNO))
            .extracting(FilaComanda::tiempoComida, FilaComanda::platillo, FilaComanda::raciones)
            .containsExactlyInAnyOrder(
                tuple(TiempoComida.DESAYUNO, "Avena " + blanda.getNombre(), 2L),
                tuple(TiempoComida.DESAYUNO, "Avena " + normal.getNombre(), 1L));
    }

    @Test
    void pendientesSoloIncluyenAsignacionesActivasSinAprobarDePacientesIngresados() {
        Piso piso = datos.piso("Piso pendientes");
        Menu menu = datos.menu("Hiposódica", TipoDieta.HIPOSODICA, EstadoMenu.ACTIVO);
        AsignacionMenu pendiente = datos.pendiente(datos.paciente(piso, "Ana", "Pendiente"), menu, nutricionista, HOY);
        AsignacionMenu aprobada = datos.aprobada(datos.paciente(piso, "Beto", "Aprobada"), menu, nutricionista, HOY, null);
        AsignacionMenu inactiva = datos.pendiente(datos.paciente(piso, "Ciro", "Inactiva"), menu, nutricionista, HOY);
        inactiva.setActiva(false);
        Paciente deAlta = datos.paciente(piso, "Dora", "Alta");
        AsignacionMenu deAltaPendiente = datos.pendiente(deAlta, menu, nutricionista, HOY);
        deAlta.setEstado(EstadoPaciente.ALTA);

        assertThat(reportes.buscarAsignacionesPendientes(EstadoPaciente.INGRESADO))
            .extracting(AsignacionMenu::getId)
            .contains(pendiente.getId())
            .doesNotContain(aprobada.getId(), inactiva.getId(), deAltaPendiente.getId());
    }

    @Test
    void sinMenuVigenteExcluyeAsignacionesVigentesEIncluyeLosDemasCasos() {
        Piso piso = datos.piso("Piso sin menú");
        Menu activo = datos.menu("Normal", TipoDieta.NORMAL, EstadoMenu.ACTIVO);
        Menu inactivo = datos.menu("Retirado", TipoDieta.NORMAL, EstadoMenu.INACTIVO);
        Paciente conVigente = datos.paciente(piso, "Ana", "Vigente");
        datos.aprobada(conVigente, activo, nutricionista, HOY.minusDays(2), null);
        Paciente sinAsignacion = datos.paciente(piso, "Beto", "SinAsignacion");
        Paciente vencida = datos.paciente(piso, "Ciro", "Vencida");
        datos.aprobada(vencida, activo, nutricionista, HOY.minusDays(9), HOY.minusDays(1));
        Paciente soloPendiente = datos.paciente(piso, "Dora", "SoloPendiente");
        datos.pendiente(soloPendiente, activo, nutricionista, HOY);
        Paciente menuInactivo = datos.paciente(piso, "Eva", "MenuInactivo");
        datos.aprobada(menuInactivo, inactivo, nutricionista, HOY.minusDays(1), null);
        Paciente futura = datos.paciente(piso, "Fito", "Futura");
        datos.aprobada(futura, activo, nutricionista, HOY.plusDays(2), null);
        Paciente desactivada = datos.paciente(piso, "Gina", "Desactivada");
        datos.aprobada(desactivada, activo, nutricionista, HOY.minusDays(1), null).setActiva(false);
        Paciente deAlta = datos.paciente(piso, "Hugo", "Alta");
        deAlta.setEstado(EstadoPaciente.ALTA);

        assertThat(reportes.buscarPacientesSinMenuVigente(EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO, HOY, piso.getId()))
            .extracting(Paciente::getApellidos)
            .containsExactlyInAnyOrder("SinAsignacion", "Vencida", "SoloPendiente", "MenuInactivo", "Futura",
                "Desactivada");
        List<Long> ids = List.of(sinAsignacion.getId(), soloPendiente.getId(), vencida.getId());
        assertThat(reportes.buscarPacientesConAsignacionPendiente(ids)).containsExactly(soloPendiente.getId());
        assertThat(reportes.buscarAsignacionesAprobadas(ids))
            .extracting(asignacion -> asignacion.getPaciente().getId())
            .containsExactly(vencida.getId());
    }
}
