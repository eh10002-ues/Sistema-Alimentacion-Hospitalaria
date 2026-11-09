package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.RacionesServidasService;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;

class RacionesServidasServiceTests {

    private final AsignacionMenuRepository asignaciones = mock(AsignacionMenuRepository.class);
    private final RacionesServidasService servicio = new RacionesServidasService(asignaciones);

    @BeforeEach
    void limpiarMocks() {
        org.mockito.Mockito.reset(asignaciones);
    }

    @Test
    void cuentaCadaComidaPorDiaEnElTraslapeDelMesYResumePorPiso() {
        YearMonth periodo = YearMonth.of(2025, 3);
        AsignacionMenu asignacion = asignacion(
            LocalDate.of(2025, 2, 27), LocalDate.of(2025, 3, 2), "P1 — Pediatría", 2);
        when(asignaciones.findAprobadasConTraslape(periodo.atDay(1), periodo.atEndOfMonth(),
            EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(asignacion));

        var resultado = servicio.generar(periodo, LocalDate.of(2025, 3, 31));

        assertThat(resultado.totalRaciones()).isEqualTo(4);
        assertThat(resultado.totalesPorPiso()).containsExactly(
            new sv.edu.ues.alimentacion_hospitalaria.reportes.dto.RacionesServidasVista.TotalPiso("P1 — Pediatría", 4));
        assertThat(resultado.filas()).hasSize(2).allSatisfy(fila -> assertThat(fila.raciones()).isEqualTo(2));
    }

    @Test
    void limitaElAcumuladoDelMesActualHastaLaFechaDeCorte() {
        YearMonth periodo = YearMonth.of(2025, 3);
        AsignacionMenu asignacion = asignacion(
            LocalDate.of(2025, 3, 1), null, "P2 — Cirugía", 1);
        when(asignaciones.findAprobadasConTraslape(periodo.atDay(1), LocalDate.of(2025, 3, 5),
            EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(asignacion));

        var resultado = servicio.generar(periodo, LocalDate.of(2025, 3, 5));

        assertThat(resultado.fechaCorte()).isEqualTo(LocalDate.of(2025, 3, 5));
        assertThat(resultado.totalRaciones()).isEqualTo(5);
    }

    @Test
    void rechazaMesesFuturosSinConsultarLaBase() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> servicio.generar(YearMonth.of(2025, 4), LocalDate.of(2025, 3, 31)))
            .withMessage("El reporte no admite meses futuros.");
        verify(asignaciones, never()).findAprobadasConTraslape(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static AsignacionMenu asignacion(LocalDate inicio, LocalDate fin, String nombrePiso,
                                             int cantidadComidas) {
        Menu menu = new Menu("Menú de prueba", TipoMenu.GENERAL, TipoDieta.NORMAL);
        menu.agregarDetalle(new MenuDetalle(TiempoComida.DESAYUNO, LocalTime.of(7, 0), "Avena"));
        if (cantidadComidas > 1) {
            menu.agregarDetalle(new MenuDetalle(TiempoComida.ALMUERZO, LocalTime.of(12, 0), "Arroz"));
        }

        String[] datosPiso = nombrePiso.split(" — ", 2);
        Piso piso = mock(Piso.class);
        when(piso.getCodigo()).thenReturn(datosPiso[0]);
        when(piso.getNombre()).thenReturn(datosPiso[1]);
        Cama cama = mock(Cama.class);
        when(cama.getPiso()).thenReturn(piso);
        Paciente paciente = mock(Paciente.class);
        when(paciente.getCama()).thenReturn(cama);

        AsignacionMenu asignacion = mock(AsignacionMenu.class);
        when(asignacion.getMenu()).thenReturn(menu);
        when(asignacion.getPaciente()).thenReturn(paciente);
        when(asignacion.getFechaInicio()).thenReturn(inicio);
        when(asignacion.getFechaFin()).thenReturn(fin);
        return asignacion;
    }
}
