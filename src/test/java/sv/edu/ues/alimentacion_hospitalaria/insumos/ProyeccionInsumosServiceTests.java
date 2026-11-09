package sv.edu.ues.alimentacion_hospitalaria.insumos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.service.RegistroBitacoraService;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaProyeccionInsumo;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaDemandaProyeccion;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.PeriodoProyeccion;
import sv.edu.ues.alimentacion_hospitalaria.insumos.persistence.ProyeccionInsumosRepository;
import sv.edu.ues.alimentacion_hospitalaria.insumos.service.ProyeccionInsumosService;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ProyeccionInsumosServiceTests {

    @Mock
    private ProyeccionInsumosRepository repository;

    @Mock
    private RegistroBitacoraService registroBitacoraService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ProyeccionInsumosService service;

    @BeforeEach
    void autenticarUsuario() {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "cocina", "n/a", List.of(new SimpleGrantedAuthority("ROLE_COCINA"))));
        Usuario actor = mock(Usuario.class);
        lenient().when(actor.getId()).thenReturn(1L);
        lenient().when(actor.isActivo()).thenReturn(true);
        lenient().when(actor.getRol()).thenReturn(Rol.ROLE_COCINA);
        lenient().when(usuarioRepository.findByNombreUsuarioIgnoreCase("cocina"))
            .thenReturn(Optional.of(actor));
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculaNetoBrutoMargenYCompraConDíasVigentesInclusivos() {
        LocalDate inicio = LocalDate.of(2026, 11, 5);
        FilaProyeccionInsumo fila = fila(
            new BigDecimal("2.00"),
            new BigDecimal("0.1000"),
            new BigDecimal("2.00"),
            inicio.plusDays(2),
            inicio.plusDays(4));
        when(repository.buscarFilasVigentes(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(fila));
        when(repository.buscarDemandaVigente(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(new FilaDemandaProyeccion(10L, 20L, inicio.plusDays(2), inicio.plusDays(4))));

        var proyeccion = service.proyectar(
            PeriodoProyeccion.SEMANAL, new BigDecimal("0.10"), inicio);

        var insumo = proyeccion.insumos().getFirst();
        assertThat(proyeccion.fechaFin()).isEqualTo(inicio.plusDays(6));
        assertThat(proyeccion.margenPorcentaje()).isEqualByComparingTo("10.00");
        assertThat(proyeccion.pacientesIncluidos()).isEqualTo(1);
        assertThat(proyeccion.racionesPorDia()).isEqualByComparingTo(
            new BigDecimal("3").divide(new BigDecimal("7"), MathContext.DECIMAL128));
        assertThat(insumo.neto()).isEqualByComparingTo("6.00");
        assertThat(insumo.bruto()).isEqualByComparingTo(
            new BigDecimal("6").divide(new BigDecimal("0.9"), MathContext.DECIMAL128));
        assertThat(insumo.totalConMargen()).isEqualByComparingTo(
            new BigDecimal("6").multiply(new BigDecimal("1.1"))
                .divide(new BigDecimal("0.9"), MathContext.DECIMAL128));
        assertThat(insumo.cantidadComprar()).isEqualByComparingTo("4");
        assertThat(insumo.costo()).isEqualByComparingTo(
            insumo.totalConMargen().multiply(new BigDecimal("0.001")));
        assertThat(proyeccion.costoTotal()).isEqualByComparingTo(insumo.costo());
        assertThat(insumo.precioPresentacion()).isEqualByComparingTo("0.002");
        assertThat(insumo.presupuestoCompra()).isEqualByComparingTo("0.01");
        assertThat(proyeccion.resumen().presupuestoTotal()).isEqualByComparingTo("0.01");
        assertThat(proyeccion.resumen().demanda()).singleElement().satisfies(total -> {
            assertThat(total.categoria()).isEqualTo("Peso");
            assertThat(total.unidad()).isEqualTo("kg");
            assertThat(total.cantidad()).isEqualByComparingTo(
                insumo.totalConMargen().multiply(new BigDecimal("0.001")));
        });
        assertThat(proyeccion.resumen().compras()).singleElement().satisfies(compra -> {
            assertThat(compra.insumo()).isEqualTo("Arroz");
            assertThat(compra.cantidad()).isEqualByComparingTo("4");
            assertThat(compra.precioPresentacion()).isEqualByComparingTo("0.002");
            assertThat(compra.subtotal()).isEqualByComparingTo("0.01");
        });
        assertThat(proyeccion.advertencias()).isEmpty();
        verify(repository).buscarFilasVigentes(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO);
        verify(repository).buscarDemandaVigente(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO);
        verify(registroBitacoraService).registrar("PROYECTAR", "insumos", null, 1L);
    }

    @Test
    void consolidaDemandaEnKilogramosYLitrosSinMezclarUnidadesIncompatibles() {
        LocalDate inicio = LocalDate.of(2026, 11, 5);
        when(repository.buscarFilasVigentes(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(
                filaUnidad(1L, "Arroz en gramos", "g", BigDecimal.ONE, inicio, inicio.plusDays(6)),
                filaUnidad(2L, "Arroz en kilos", "kg", BigDecimal.ONE, inicio, inicio.plusDays(6)),
                filaUnidad(3L, "Leche en mililitros", "ml", BigDecimal.ONE, inicio, inicio.plusDays(6)),
                filaUnidad(4L, "Leche en litros", "L", BigDecimal.ONE, inicio, inicio.plusDays(6))));
        when(repository.buscarDemandaVigente(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(new FilaDemandaProyeccion(10L, 20L, inicio, inicio)));

        var proyeccion = service.proyectar(PeriodoProyeccion.SEMANAL, BigDecimal.ZERO, inicio);

        assertThat(proyeccion.resumen().demanda()).hasSize(2);
        assertThat(proyeccion.resumen().demanda())
            .filteredOn(total -> total.unidad().equals("kg"))
            .singleElement()
            .satisfies(total -> assertThat(total.cantidad()).isEqualByComparingTo("7.007"));
        assertThat(proyeccion.resumen().demanda())
            .filteredOn(total -> total.unidad().equals("L"))
            .singleElement()
            .satisfies(total -> assertThat(total.cantidad()).isEqualByComparingTo("7.007"));
    }

    @Test
    void asumeMermaCeroYAdvierteCuandoMermaOPresentacionNoEstanConfiguradas() {
        LocalDate inicio = LocalDate.of(2026, 11, 5);
        FilaProyeccionInsumo fila = fila(
            BigDecimal.ONE, null, null, inicio, inicio);
        when(repository.buscarFilasVigentes(
            inicio, inicio.plusDays(29), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(fila));
        when(repository.buscarDemandaVigente(
            inicio, inicio.plusDays(29), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(new FilaDemandaProyeccion(10L, 20L, inicio, inicio)));

        var proyeccion = service.proyectar(
            PeriodoProyeccion.MENSUAL, new BigDecimal("0.10"), inicio);

        var insumo = proyeccion.insumos().getFirst();
        assertThat(proyeccion.fechaFin()).isEqualTo(inicio.plusDays(29));
        assertThat(proyeccion.racionesPorDia()).isEqualByComparingTo(
            BigDecimal.ONE.divide(new BigDecimal("30"), MathContext.DECIMAL128));
        assertThat(insumo.neto()).isEqualByComparingTo("1");
        assertThat(insumo.bruto()).isEqualByComparingTo("1");
        assertThat(insumo.totalConMargen()).isEqualByComparingTo("1.1");
        assertThat(insumo.cantidadComprar()).isNull();
        assertThat(proyeccion.resumen().presupuestoTotal()).isNull();
        assertThat(insumo.advertencias()).containsExactly(
            "Merma no registrada; se consideró 0%.",
            "Configure una cantidad de presentación mayor que cero para calcular unidades de compra.");
        verify(registroBitacoraService).registrar("PROYECTAR", "insumos", null, 1L);
    }

    @Test
    void rechazaMermaFueraDelRangoPermitido() {
        LocalDate inicio = LocalDate.of(2026, 11, 5);
        when(repository.buscarFilasVigentes(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(fila(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, inicio, inicio)));
        when(repository.buscarDemandaVigente(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(new FilaDemandaProyeccion(10L, 20L, inicio, inicio)));

        assertThatThrownBy(() -> service.proyectar(
            PeriodoProyeccion.SEMANAL, new BigDecimal("0.10"), inicio))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("La merma del insumo debe ser una fracción entre 0 y 1.");
    }

    @Test
    void advierteCostoFaltanteYNoInformaUnPresupuestoParcialComoTotal() {
        LocalDate inicio = LocalDate.of(2026, 11, 5);
        FilaProyeccionInsumo fila = new FilaProyeccionInsumo(
            1L, 10L, 20L, "Arroz", "g", BigDecimal.ONE, BigDecimal.ZERO,
            "saco", new BigDecimal("1000"), null, inicio, inicio);
        when(repository.buscarFilasVigentes(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(fila));
        when(repository.buscarDemandaVigente(
            inicio, inicio.plusDays(6), EstadoPaciente.INGRESADO, EstadoMenu.ACTIVO))
            .thenReturn(List.of(new FilaDemandaProyeccion(10L, 20L, inicio, inicio)));

        var proyeccion = service.proyectar(
            PeriodoProyeccion.SEMANAL, new BigDecimal("0.10"), inicio);

        assertThat(proyeccion.insumos().getFirst().costo()).isNull();
        assertThat(proyeccion.insumos().getFirst().advertencias())
            .contains("Costo unitario no registrado; no se puede calcular el costo de este insumo.");
        assertThat(proyeccion.costoTotal()).isNull();
        assertThat(proyeccion.advertencias())
            .contains("Falta registrar costo unitario en uno o más insumos; el costo total de consumo no está disponible.",
                "Faltan presentaciones o costos unitarios; el presupuesto total de compra no está disponible.");
        assertThat(proyeccion.resumen().presupuestoTotal()).isNull();
    }

    private FilaProyeccionInsumo fila(BigDecimal cantidadPorRacion, BigDecimal merma,
                                     BigDecimal cantidadPresentacion,
                                     LocalDate fechaInicio, LocalDate fechaFin) {
        return new FilaProyeccionInsumo(
            1L, 10L, 20L, "Arroz", "g", cantidadPorRacion, merma, "saco",
            cantidadPresentacion, new BigDecimal("0.001"), fechaInicio, fechaFin);
    }

    private FilaProyeccionInsumo filaUnidad(Long id, String nombre, String unidad,
                                            BigDecimal cantidadPorRacion, LocalDate fechaInicio,
                                            LocalDate fechaFin) {
        return new FilaProyeccionInsumo(
            id, 10L, 20L, nombre, unidad, cantidadPorRacion, BigDecimal.ZERO,
            "bolsa", BigDecimal.TEN, new BigDecimal("0.01"), fechaInicio, fechaFin);
    }
}
