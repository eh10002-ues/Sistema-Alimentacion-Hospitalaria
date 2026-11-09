package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.TestcontainersConfiguration;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.*;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilasReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte.EstadoOcupacion;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.ConsultaReportesService;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/** HU-02 paso 3: proyecciones reales sobre MySQL descartable y DTO por rol. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@WithMockUser(roles = "ADMIN")
class ConsultaReportesTests {
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 8);
    @Autowired ConsultaReportesService servicio;
    @Autowired PisoRepository pisos;
    @Autowired CamaRepository camas;
    @Autowired PacienteRepository pacientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired MenuRepository menus;
    @Autowired AsignacionMenuRepository asignaciones;
    @Autowired EntityManager em;
    private Piso piso;
    private Usuario responsable;

    @BeforeEach
    void preparar() {
        piso = pisos.save(new Piso("R" + UUID.randomUUID().toString().substring(0, 5), "Piso sintético"));
        responsable = usuarios.save(new Usuario("reporte." + UUID.randomUUID().toString().substring(0, 8),
            "hash-no-utilizable", "Responsable sintético", Rol.ROLE_MEDICO, true));
    }

    private Cama cama(String numero) { return camas.save(new Cama(piso, numero)); }

    private Paciente paciente(String codigo, Cama cama, EstadoPaciente estado) {
        Paciente paciente = new Paciente(codigo, "Paciente", "Sintético", LocalDate.of(1990, 1, 1), new BigDecimal("65"));
        paciente.setAlturaCm(new BigDecimal("170"));
        paciente.setCama(cama);
        paciente.setEstado(estado);
        return pacientes.save(paciente);
    }

    private Menu menu(String nombre, EstadoMenu estado) {
        Menu menu = new Menu(nombre, TipoMenu.GENERAL, TipoDieta.BLANDA);
        menu.setEstado(estado);
        menu.agregarDetalle(new MenuDetalle(TiempoComida.CENA, LocalTime.of(18, 0), "Cena sintética"));
        menu.agregarDetalle(new MenuDetalle(TiempoComida.DESAYUNO, LocalTime.of(7, 0), "Desayuno sintético"));
        return menus.save(menu);
    }

    private AsignacionMenu asignar(Paciente paciente, Menu menu, LocalDate inicio, LocalDate fin, boolean aprobada) {
        AsignacionMenu asignacion = new AsignacionMenu(paciente, menu, responsable, inicio);
        asignacion.setFechaFin(fin);
        if (aprobada) {
            asignacion.setAprobadoPor(responsable);
            asignacion.setFechaAprobacion(inicio);
        }
        return asignaciones.save(asignacion);
    }

    private FiltrosReporte.Asignaciones filtroAsignaciones() {
        return new FiltrosReporte.Asignaciones(0, "", null, FECHA, null);
    }

    @Test
    void pacientesIncluyeSinCamaYDistingueUbicacionHistorica() {
        Cama cama = cama("01");
        paciente("A-01", cama, EstadoPaciente.INGRESADO);
        paciente("A-02", cama, EstadoPaciente.ALTA);
        paciente("A-03", null, EstadoPaciente.ALTA);
        em.flush(); em.clear();
        var resultado = servicio.pacientes(new FiltrosReporte.Pacientes(0, "", null, null));
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).extracting(FilasReporte.Paciente::codigo).containsExactly("A-01", "A-02", "A-03");
        assertThat(resultado.getContent().get(1).ubicacionHistorica()).isTrue();
        assertThat(resultado.getContent().get(2).cama()).isNull();
        assertThat(servicio.pacientes(new FiltrosReporte.Pacientes(0, "Paciente Sintético", piso.getId(), EstadoPaciente.INGRESADO)))
            .extracting(FilasReporte.Paciente::codigo).containsExactly("A-01");
    }

    @Test
    void paginacionDePacientesNoRepiteFilasYTieneConteoCorrecto() {
        for (int i = 0; i < 23; i++) { paciente("P-%02d".formatted(i), null, EstadoPaciente.ALTA); }
        em.flush(); em.clear();
        var primera = servicio.pacientes(new FiltrosReporte.Pacientes(0, "", null, null));
        var segunda = servicio.pacientes(new FiltrosReporte.Pacientes(1, "", null, null));
        assertThat(primera.getContent()).hasSize(20);
        assertThat(segunda.getContent()).hasSize(3).doesNotContainAnyElementsOf(primera.getContent());
        assertThat(segunda.getTotalElements()).isEqualTo(23);
        var fuera = servicio.pacientes(new FiltrosReporte.Pacientes(3, "", null, null));
        assertThat(fuera).isEmpty();
        assertThat(fuera.getTotalElements()).isEqualTo(23);
    }

    @Test
    void busquedaEscapaPorcentajesYGuionesBajos() {
        paciente("A_01", null, EstadoPaciente.ALTA);
        paciente("AB01", null, EstadoPaciente.ALTA);
        assertThat(servicio.pacientes(new FiltrosReporte.Pacientes(0, "_", null, null)))
            .extracting(FilasReporte.Paciente::codigo).containsExactly("A_01");
        assertThat(servicio.pacientes(new FiltrosReporte.Pacientes(0, "%", null, null))).isEmpty();
    }

    @Test
    void asignacionesSoloIncluyeAprobadasActivasVigentesDeIngresadosYMenusActivos() {
        Paciente p = paciente("VIGENTE", cama("01"), EstadoPaciente.INGRESADO);
        Menu menu = menu("Menú vigente", EstadoMenu.ACTIVO);
        asignar(p, menu, FECHA, FECHA, true);
        asignar(p, menu, FECHA.minusDays(1), null, true);
        asignar(p, menu, FECHA, null, false);
        asignar(p, menu, FECHA.plusDays(1), null, true);
        asignar(p, menu, FECHA.minusDays(3), FECHA.minusDays(1), true);
        asignar(p, menu, FECHA, null, true).setActiva(false);
        asignar(p, menu("Retirado", EstadoMenu.INACTIVO), FECHA, null, true);
        asignar(paciente("ALTA", null, EstadoPaciente.ALTA), menu, FECHA, null, true);
        em.flush(); em.clear();
        var resultado = servicio.asignaciones(filtroAsignaciones());
        assertThat(resultado.pagina()).hasSize(2);
        assertThat(resultado.pagina().getTotalElements()).isEqualTo(2);
        var fila = (FilasReporte.AsignacionClinica) resultado.pagina().getContent().getFirst();
        assertThat(fila.responsable()).isEqualTo("Responsable sintético");
        assertThat(fila.dieta()).isEqualTo(TipoDieta.BLANDA);
        assertThat(fila.tiempos()).extracting(FilasReporte.Tiempo::horaServicio)
            .containsExactly(LocalTime.of(7, 0), LocalTime.of(18, 0));
        assertThat(servicio.asignaciones(new FiltrosReporte.Asignaciones(0, "VIGENTE", piso.getId(), FECHA, TipoDieta.NORMAL)).pagina()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "MEDICO", "NUTRICIONISTA", "ENCARGADO_PISO", "COCINA"})
    void asignacionesDevuelveModelosDiferentesSegunElRol(String rol) {
        asignar(paciente("PAC", cama("01"), EstadoPaciente.INGRESADO), menu("Menú", EstadoMenu.ACTIVO), FECHA, null, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("prueba", "",
            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
        var fila = servicio.asignaciones(filtroAsignaciones()).pagina().getContent().getFirst();
        if (rol.equals("COCINA")) {
            assertThat(fila).isInstanceOf(FilasReporte.AsignacionCocina.class);
            assertThat(java.util.Arrays.stream(fila.getClass().getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("responsable", "inicio", "fin", "estado");
        } else if (rol.equals("ENCARGADO_PISO")) {
            assertThat(fila).isInstanceOf(FilasReporte.AsignacionPiso.class);
            assertThat(java.util.Arrays.stream(fila.getClass().getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("responsable", "inicio", "fin", "tiempos", "fecha");
        } else { assertThat(fila).isInstanceOf(FilasReporte.AsignacionClinica.class); }
    }

    @Test
    void menusNoSeDuplicanPorPacienteYFiltranTiempoDietaYFecha() {
        Menu menu = menu("Blanda", EstadoMenu.ACTIVO);
        asignar(paciente("PAC-1", cama("01"), EstadoPaciente.INGRESADO), menu, FECHA, null, true);
        asignar(paciente("PAC-2", cama("02"), EstadoPaciente.INGRESADO), menu, FECHA, null, true);
        menu("Sin asignación", EstadoMenu.ACTIVO);
        em.flush(); em.clear();
        var resultado = servicio.menus(new FiltrosReporte.Menus(0, FECHA, null, null));
        assertThat(resultado.pagina().getTotalElements()).isEqualTo(2);
        assertThat(resultado.pagina()).extracting(FilasReporte.Menu::tiempoComida)
            .containsExactly(TiempoComida.DESAYUNO, TiempoComida.CENA);
        assertThat(resultado.pagina()).extracting(FilasReporte.Menu::horaServicio)
            .containsExactly(LocalTime.of(7, 0), LocalTime.of(18, 0));
        assertThat(servicio.menus(new FiltrosReporte.Menus(0, FECHA, TiempoComida.CENA, TipoDieta.BLANDA)).pagina()).hasSize(1);
        assertThat(servicio.menus(new FiltrosReporte.Menus(0, FECHA.minusDays(1), null, null)).pagina()).isEmpty();
        assertThat(servicio.menus(new FiltrosReporte.Menus(0, FECHA, null, TipoDieta.NORMAL)).pagina()).isEmpty();
    }

    @Test
    void ocupacionCuentaSoloIngresadosYConservaTodosLosOcupantesInconsistentes() {
        Cama libre = cama("01");
        paciente("HISTORICO", libre, EstadoPaciente.ALTA);
        Cama ocupada = cama("02");
        paciente("ACTUAL", ocupada, EstadoPaciente.INGRESADO);
        Cama inconsistente = cama("03");
        paciente("DOBLE-1", inconsistente, EstadoPaciente.INGRESADO);
        paciente("DOBLE-2", inconsistente, EstadoPaciente.INGRESADO);
        em.flush(); em.clear();
        var resultado = servicio.ocupacion(new FiltrosReporte.Ocupacion(0, piso.getId(), "", null));
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado).extracting(FilasReporte.Ocupacion::estado)
            .containsExactly(EstadoOcupacion.DISPONIBLE, EstadoOcupacion.OCUPADA, EstadoOcupacion.INCONSISTENTE);
        assertThat(resultado.getContent().getFirst().ocupantes()).isEmpty();
        assertThat(resultado.getContent().get(2).ocupantes()).extracting(FilasReporte.Ocupante::codigo)
            .containsExactly("DOBLE-1", "DOBLE-2");
        for (EstadoOcupacion estado : EstadoOcupacion.values()) {
            assertThat(servicio.ocupacion(new FiltrosReporte.Ocupacion(0, piso.getId(), "", estado)))
                .hasSize(1).extracting(FilasReporte.Ocupacion::estado).containsExactly(estado);
        }
        assertThat(servicio.ocupacion(new FiltrosReporte.Ocupacion(0, piso.getId(), "02", null)))
            .extracting(FilasReporte.Ocupacion::cama).containsExactly("02");
    }

    @Test
    void validacionDePisoPaginaYFiltrosNoSeConfundeConListaVacia() {
        assertThatThrownBy(() -> servicio.pacientes(new FiltrosReporte.Pacientes(-1, "", null, null)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.ocupacion(new FiltrosReporte.Ocupacion(0, Long.MAX_VALUE, "", null)))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("El piso seleccionado no existe.");
        assertThatThrownBy(() -> servicio.asignaciones(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(servicio.menus(new FiltrosReporte.Menus(0, null, null, null)).fechaConsulta())
            .isEqualTo(LocalDate.now(java.time.ZoneId.of("America/El_Salvador")));
    }

    @Test
    @WithMockUser(roles = "COCINA")
    void cocinaNoConsultaPacientesNiOcupacionAunqueLlameAlServicio() {
        assertThatThrownBy(() -> servicio.pacientes(new FiltrosReporte.Pacientes(0, "", null, null)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.ocupacion(new FiltrosReporte.Ocupacion(0, null, "", null)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void numeroDeConsultasDeAsignacionesNoCrecePorCadaFilaOTiempo() {
        Menu menu = menu("Compartido", EstadoMenu.ACTIVO);
        for (int i = 0; i < 23; i++) {
            asignar(paciente("PAC-%02d".formatted(i), null, EstadoPaciente.INGRESADO), menu, FECHA, null, true);
        }
        em.flush(); em.clear();
        var estadisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean habilitadas = estadisticas.isStatisticsEnabled();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            var pagina = servicio.asignaciones(filtroAsignaciones()).pagina();
            assertThat(pagina.getTotalElements()).isEqualTo(23);
            assertThat(pagina).hasSize(20);
            assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(3);
            assertThat(estadisticas.getEntityLoadCount()).isZero();
            assertThat(servicio.asignaciones(new FiltrosReporte.Asignaciones(1, "", null, FECHA, null)).pagina()).hasSize(3);
        } finally { estadisticas.setStatisticsEnabled(habilitadas); }
    }

    @Test
    void ocupacionPaginaCamasAntesDeCargarOcupantes() {
        for (int i = 0; i < 23; i++) { cama("C-%02d".formatted(i)); }
        var pagina = servicio.ocupacion(new FiltrosReporte.Ocupacion(1, piso.getId(), "", EstadoOcupacion.DISPONIBLE));
        assertThat(pagina.getTotalElements()).isEqualTo(23);
        assertThat(pagina).hasSize(3);
    }

    @Test
    void paginacionDeMenusConservaConteoYOrdenSinDuplicarPorAsignaciones() {
        Paciente paciente = paciente("PAC", null, EstadoPaciente.INGRESADO);
        for (int i = 0; i < 11; i++) {
            Menu menu = menu("Menú %02d".formatted(i), EstadoMenu.ACTIVO);
            asignar(paciente, menu, FECHA, null, true);
            asignar(paciente, menu, FECHA, null, true);
        }
        em.flush(); em.clear();
        var primera = servicio.menus(new FiltrosReporte.Menus(0, FECHA, null, null)).pagina();
        var segunda = servicio.menus(new FiltrosReporte.Menus(1, FECHA, null, null)).pagina();
        assertThat(primera).hasSize(20);
        assertThat(segunda.getTotalElements()).isEqualTo(22);
        assertThat(segunda).hasSize(2).doesNotContainAnyElementsOf(primera.getContent());
        assertThat(primera.getContent().subList(0, 11)).extracting(FilasReporte.Menu::tiempoComida)
            .containsOnly(TiempoComida.DESAYUNO);
        assertThat(segunda).extracting(FilasReporte.Menu::nombre).containsExactly("Menú 09", "Menú 10");
    }

    @Test
    void ocupacionNoCargaEntidadesNiUnaConsultaPorCama() {
        for (int i = 0; i < 23; i++) {
            paciente("PAC-%02d".formatted(i), cama("C-%02d".formatted(i)), EstadoPaciente.INGRESADO);
        }
        em.flush(); em.clear();
        var estadisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean habilitadas = estadisticas.isStatisticsEnabled();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            var pagina = servicio.ocupacion(new FiltrosReporte.Ocupacion(0, null, "", null));
            assertThat(pagina).hasSize(20);
            assertThat(pagina).allSatisfy(fila -> assertThat(fila.ocupantes()).hasSize(1));
            assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(3);
            assertThat(estadisticas.getEntityLoadCount()).isZero();
        } finally { estadisticas.setStatisticsEnabled(habilitadas); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"OTRO", "COCINA,ADMIN"})
    void consultasNoAceptanRolesDesconocidosNiMultiples(String rol) {
        var autoridades = java.util.Arrays.stream(rol.split(","))
            .map(valor -> new SimpleGrantedAuthority("ROLE_" + valor)).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("prueba", "", autoridades));
        assertThatThrownBy(() -> servicio.asignaciones(filtroAsignaciones())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.menus(new FiltrosReporte.Menus(0, FECHA, null, null)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void consultasRequierenAutenticacion() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> servicio.asignaciones(filtroAsignaciones()))
            .isInstanceOf(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class);
    }

}
