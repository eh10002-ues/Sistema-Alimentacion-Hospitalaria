package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.ValidacionConsultaReportes;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;

class ValidacionConsultaReportesTests {
    private static ValidatorFactory fabrica;
    private PisoRepository pisos;
    private ValidacionConsultaReportes validacion;

    @BeforeAll
    static void iniciar() { fabrica = Validation.buildDefaultValidatorFactory(); }

    @AfterAll
    static void cerrar() { fabrica.close(); }

    @BeforeEach
    void preparar() {
        pisos = mock(PisoRepository.class);
        validacion = new ValidacionConsultaReportes(fabrica.getValidator(), pisos);
    }

    @Test
    void rechazaFiltrosAusentesOInvalidosAntesDeAccederALaBase() {
        assertThatThrownBy(() -> validacion.validar(null, null))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("Indique los filtros del reporte.");
        assertThatThrownBy(() -> validacion.validar(new FiltrosReporte.Pacientes(-1, "", null, null), null))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("La página no puede ser negativa.");
        verifyNoInteractions(pisos);
    }

    @Test
    void pisoInexistenteEsUnErrorDeFiltroNoUnResultadoVacio() {
        assertThatThrownBy(() -> validacion.validar(new FiltrosReporte.Pacientes(0, "", 5L, null), 5L))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("El piso seleccionado no existe.");
    }

    @Test
    void admitePisoExistenteYFiltroSinPiso() {
        validacion.validar(new FiltrosReporte.Pacientes(0, "", null, null), null);
        verifyNoInteractions(pisos);
        when(pisos.existsById(5L)).thenReturn(true);
        validacion.validar(new FiltrosReporte.Pacientes(0, "", 5L, null), 5L);
    }

    @Test
    void paginacionTieneLimiteFijoYRechazaValoresNegativos() {
        assertThat(validacion.pagina(3).getPageSize()).isEqualTo(20);
        assertThat(validacion.pagina(3).getOffset()).isEqualTo(60);
        assertThatThrownBy(() -> validacion.pagina(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
