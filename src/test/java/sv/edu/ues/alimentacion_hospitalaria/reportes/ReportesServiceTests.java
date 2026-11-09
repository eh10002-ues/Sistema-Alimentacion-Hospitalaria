package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.AlergiasSeverasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.AsignacionesPendientesReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.ComandasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.PacientesSinMenuReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.ReportesService;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
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
@WithMockUser(username = "nutri.reportes", roles = "NUTRICIONISTA")
class ReportesServiceTests {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 15);

    @Autowired private ReportesService servicio;
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
            .usuario("nutri.reportes.servicio", Rol.ROLE_NUTRICIONISTA);
    }

    @Test
    void alergiasSeverasSeAgrupanPorPisoConEtiquetaGraveYNotaDeAlcance() {
        Piso piso = datos.piso("Piso norte");
        Paciente ana = datos.conAlergia(datos.paciente(piso, "Ana", "Ramírez"), "Maní", SeveridadAlergia.SEVERA);
        datos.conAlergia(ana, "Penicilina", SeveridadAlergia.SEVERA);
        datos.conAlergia(datos.paciente(piso, "Beto", "Soto"), "Mariscos", SeveridadAlergia.SEVERA);

        AlergiasSeverasReporte reporte = servicio.alergiasSeveras(piso.getId());

        String etiquetaPiso = piso.getCodigo() + " — Piso norte";
        assertThat(reporte.pisos()).singleElement().satisfies(grupo -> {
            assertThat(grupo.piso()).isEqualTo(etiquetaPiso);
            assertThat(grupo.filas())
                .extracting(AlergiasSeverasReporte.Fila::paciente, AlergiasSeverasReporte.Fila::alergia,
                    AlergiasSeverasReporte.Fila::severidad)
                .containsExactlyInAnyOrder(
                    tuple("Ana Ramírez", "Maní", "Grave"),
                    tuple("Ana Ramírez", "Penicilina", "Grave"),
                    tuple("Beto Soto", "Mariscos", "Grave"));
            // Orden de lectura en piso: cama y luego nombre de la alergia.
            assertThat(grupo.filas()).isSortedAccordingTo(
                Comparator.comparing(AlergiasSeverasReporte.Fila::cama)
                    .thenComparing(AlergiasSeverasReporte.Fila::alergia));
        });
        assertThat(reporte.totalPacientes()).isEqualTo(2);
        assertThat(reporte.totalAlergias()).isEqualTo(3);
        assertThat(reporte.encabezado().codigo()).isEqualTo("RE-01");
        assertThat(reporte.encabezado().titulo()).isEqualTo("Pacientes con alergias severas registradas por piso");
        assertThat(reporte.encabezado().filtros()).contains("Piso: " + etiquetaPiso);
        assertThat(reporte.encabezado().nota()).contains("no distingue el tipo de alergia");
        assertThat(reporte.encabezado().generadoPor()).isEqualTo("nutri.reportes");
    }

    @Test
    void alergiasDePacientesSinCamaQuedanEnUnGrupoPropio() {
        Paciente sinCama = datos.conAlergia(datos.paciente(null, "Ciro", "SinCama"), "Huevo", SeveridadAlergia.SEVERA);

        assertThat(servicio.alergiasSeveras(null).pisos())
            .filteredOn(grupo -> grupo.filas().stream().anyMatch(fila -> fila.codigo().equals(sinCama.getCodigo())))
            .singleElement()
            .satisfies(grupo -> assertThat(grupo.piso()).isEqualTo("Sin cama asignada"));
    }

    @Test
    void comandasSiguenElOrdenDelDiaConSubtotalesYTotalGeneral() {
        Piso piso = datos.piso("Piso cocina");
        Menu blanda = datos.menu("Blanda", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Menu normal = datos.menu("Normal", TipoDieta.NORMAL, EstadoMenu.ACTIVO);
        datos.aprobada(datos.paciente(piso, "Ana", "Uno"), blanda, nutricionista, HOY, null);
        datos.aprobada(datos.paciente(piso, "Beto", "Dos"), blanda, nutricionista, HOY, null);
        datos.aprobada(datos.paciente(piso, "Ciro", "Tres"), normal, nutricionista, HOY, null);

        ComandasReporte reporte = servicio.comandas(HOY, piso.getId(), null);

        // Las horas de prueba están desordenadas (cena 05:00, desayuno 21:00): el orden sale del tiempo de comida.
        assertThat(reporte.tiempos())
            .extracting(ComandasReporte.GrupoTiempo::tiempoComida, ComandasReporte.GrupoTiempo::subtotal)
            .containsExactly(tuple("Desayuno", 3L), tuple("Almuerzo", 3L), tuple("Cena", 3L));
        assertThat(reporte.tiempos().getFirst().filas())
            .extracting(ComandasReporte.Fila::platillo, ComandasReporte.Fila::regimen, ComandasReporte.Fila::raciones)
            .containsExactly(
                tuple("Avena " + blanda.getNombre(), "Blanda", 2L),
                tuple("Avena " + normal.getNombre(), "Normal", 1L));
        assertThat(reporte.totalGeneral()).isEqualTo(9);
        assertThat(reporte.encabezado().filtros()).contains("Fecha: 15/03/2026", "Tiempo de comida: Todos");
    }

    @Test
    void comandasNoIncluyenHorasNiDatosDePacientes() {
        Set<Class<?>> tipos = new HashSet<>();
        for (Class<?> registro : List.of(ComandasReporte.class, ComandasReporte.GrupoTiempo.class,
                ComandasReporte.Fila.class)) {
            Arrays.stream(registro.getRecordComponents()).map(RecordComponent::getType).forEach(tipos::add);
        }
        assertThat(tipos).doesNotContain(LocalTime.class);
        assertThat(Arrays.stream(ComandasReporte.Fila.class.getRecordComponents()).map(RecordComponent::getName))
            .containsExactly("platillo", "menu", "regimen", "piso", "raciones");
    }

    @Test
    void asignacionesPendientesMuestranQuienAsignoYAclaranQueNoSonPrescripciones() {
        Piso piso = datos.piso("Piso pendientes");
        Menu menu = datos.menu("Hiposódica", TipoDieta.HIPOSODICA, EstadoMenu.ACTIVO);
        Paciente paciente = datos.paciente(piso, "Luis", "Herrera");
        datos.pendiente(paciente, menu, nutricionista, HOY.plusDays(1));

        AsignacionesPendientesReporte reporte = servicio.asignacionesPendientes();

        assertThat(reporte.filas()).filteredOn(fila -> fila.codigo().equals(paciente.getCodigo()))
            .singleElement().satisfies(fila -> {
                assertThat(fila.paciente()).isEqualTo("Luis Herrera");
                assertThat(fila.menu()).isEqualTo(menu.getNombre());
                assertThat(fila.regimen()).isEqualTo("Hiposódica");
                assertThat(fila.fechaInicio()).isEqualTo(HOY.plusDays(1));
                assertThat(fila.asignadoPor()).isEqualTo("Usuario nutri.reportes.servicio (Nutricionista)");
                assertThat(fila.solicitadaEl()).isNotNull();
            });
        assertThat(reporte.encabezado().titulo()).isEqualTo("Asignaciones de menú pendientes de aprobación");
        assertThat(reporte.encabezado().nota())
            .contains("asignaciones de menú pendientes de aprobación", "No corresponde a prescripciones dietéticas");
    }

    @Test
    void pacientesSinMenuIndicanPendientesYUltimoMenuAprobado() {
        Piso piso = datos.piso("Piso RE-04");
        Menu anterior = datos.menu("Líquida", TipoDieta.LIQUIDA, EstadoMenu.ACTIVO);
        Menu reciente = datos.menu("Blanda", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Paciente vencido = datos.paciente(piso, "Ana", "Vencido");
        datos.aprobada(vencido, anterior, nutricionista, HOY.minusDays(20), HOY.minusDays(11));
        datos.aprobada(vencido, reciente, nutricionista, HOY.minusDays(10), HOY.minusDays(1));
        Paciente soloPendiente = datos.paciente(piso, "Beto", "Pendiente");
        datos.pendiente(soloPendiente, reciente, nutricionista, HOY);
        Paciente sinAsignacion = datos.paciente(piso, "Ciro", "Nuevo");
        datos.aprobada(datos.paciente(piso, "Dora", "Vigente"), reciente, nutricionista, HOY, null);

        PacientesSinMenuReporte reporte = servicio.pacientesSinMenu(HOY, piso.getId());

        assertThat(reporte.filas())
            .extracting(PacientesSinMenuReporte.Fila::codigo, PacientesSinMenuReporte.Fila::asignacionPendiente,
                PacientesSinMenuReporte.Fila::ultimoMenu, PacientesSinMenuReporte.Fila::ultimoMenuFin)
            .containsExactlyInAnyOrder(
                tuple(vencido.getCodigo(), false, reciente.getNombre(), HOY.minusDays(1)),
                tuple(soloPendiente.getCodigo(), true, null, null),
                tuple(sinAsignacion.getCodigo(), false, null, null));
        assertThat(reporte.conAsignacionPendiente()).isEqualTo(1);
        assertThat(reporte.encabezado().nota()).contains("La prescripción dietética no se considera asignación de menú");
    }

    @Test
    void pacientesSinMenuSonElComplementoDeLaConsultaPorRegimenDeSah6() {
        Piso piso = datos.piso("Piso complemento");
        Menu blanda = datos.menu("Blanda", TipoDieta.BLANDA, EstadoMenu.ACTIVO);
        Menu normal = datos.menu("Normal", TipoDieta.NORMAL, EstadoMenu.ACTIVO);
        Menu inactivo = datos.menu("Inactivo", TipoDieta.PAPILLA, EstadoMenu.INACTIVO);
        Set<Long> todos = new HashSet<>();
        Paciente a = datos.paciente(piso, "Ana", "A");
        datos.aprobada(a, blanda, nutricionista, HOY, null);
        Paciente b = datos.paciente(piso, "Beto", "B");
        datos.aprobada(b, normal, nutricionista, HOY.minusDays(4), HOY);
        Paciente c = datos.paciente(piso, "Ciro", "C");
        datos.aprobada(c, inactivo, nutricionista, HOY, null);
        Paciente d = datos.paciente(piso, "Dora", "D");
        datos.pendiente(d, blanda, nutricionista, HOY);
        Paciente e = datos.paciente(piso, "Eva", "E");
        List.of(a, b, c, d, e).forEach(paciente -> todos.add(paciente.getId()));

        Set<Long> conMenu = new HashSet<>();
        for (TipoDieta tipoDieta : TipoDieta.values()) {
            asignaciones.findVigentesPorTipoDieta(tipoDieta, EstadoMenu.ACTIVO, EstadoPaciente.INGRESADO, HOY)
                .forEach(asignacion -> conMenu.add(asignacion.getPaciente().getId()));
        }
        conMenu.retainAll(todos);
        Set<Long> sinMenu = new HashSet<>();
        servicio.pacientesSinMenu(HOY, piso.getId()).filas().forEach(fila ->
            pacientes.findByCodigo(fila.codigo()).ifPresent(paciente -> sinMenu.add(paciente.getId())));

        assertThat(conMenu).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(sinMenu).containsExactlyInAnyOrder(c.getId(), d.getId(), e.getId());
        assertThat(conMenu).doesNotContainAnyElementsOf(sinMenu);
    }

    @Test
    void unPisoInexistenteSeRechaza() {
        assertThatThrownBy(() -> servicio.alergiasSeveras(-1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> servicio.comandas(HOY, -1L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "COCINA")
    void cocinaSoloAccedeALasComandas() {
        assertThat(servicio.comandas(HOY, null, null)).isNotNull();
        assertThatThrownBy(() -> servicio.alergiasSeveras(null)).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> servicio.asignacionesPendientes()).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> servicio.pacientesSinMenu(HOY, null)).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void elMedicoNoAccedeALasComandas() {
        assertThat(servicio.alergiasSeveras(null)).isNotNull();
        assertThat(servicio.asignacionesPendientes()).isNotNull();
        assertThat(servicio.pacientesSinMenu(HOY, null)).isNotNull();
        assertThatThrownBy(() -> servicio.comandas(HOY, null, null)).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_PISO")
    void elEncargadoDePisoNoAccedeANingunReporte() {
        assertThatThrownBy(() -> servicio.pisos()).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> servicio.alergiasSeveras(null)).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> servicio.comandas(HOY, null, null)).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> servicio.asignacionesPendientes()).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> servicio.pacientesSinMenu(HOY, null)).isInstanceOf(AuthorizationDeniedException.class);
    }
}
