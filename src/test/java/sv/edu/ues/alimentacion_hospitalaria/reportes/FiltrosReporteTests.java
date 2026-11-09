package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;

class FiltrosReporteTests {
    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void preparar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() { fabrica.close(); }

    @Test
    void filtrosOpcionalesNoRestringenLaConsultaYNormalizanTexto() {
        var pacientes = new FiltrosReporte.Pacientes(0, "  PAC-01  ", null, null);
        assertThat(pacientes.buscar()).isEqualTo("PAC-01");
        assertThat(validador.validate(pacientes)).isEmpty();
        var asignaciones = new FiltrosReporte.Asignaciones(0, null, null, null, null);
        assertThat(asignaciones.buscar()).isEmpty();
        assertThat(validador.validate(asignaciones)).isEmpty();
        assertThat(validador.validate(new FiltrosReporte.Menus(0, LocalDate.of(2026, 11, 8), null, null))).isEmpty();
        var ocupacion = new FiltrosReporte.Ocupacion(0, null, " C-01 ", null);
        assertThat(ocupacion.cama()).isEqualTo("C-01");
        assertThat(validador.validate(ocupacion)).isEmpty();
    }

    @Test
    void rechazaPaginasNegativasEnLosCuatroReportes() {
        for (Object filtro : new Object[]{new FiltrosReporte.Pacientes(-1, null, null, null),
                new FiltrosReporte.Asignaciones(-1, null, null, null, null),
                new FiltrosReporte.Menus(-1, null, null, null),
                new FiltrosReporte.Ocupacion(-1, null, null, null)}) {
            assertThat(validador.validate(filtro)).extracting(error -> error.getPropertyPath().toString())
                .containsExactly("pagina");
        }
    }

    @Test
    void rechazaIdentificadoresYTextosFueraDeLimite() {
        assertThat(validador.validate(new FiltrosReporte.Pacientes(0, "x".repeat(101), 0L, null)))
            .extracting(error -> error.getPropertyPath().toString()).containsExactlyInAnyOrder("buscar", "pisoId");
        assertThat(validador.validate(new FiltrosReporte.Asignaciones(0, "x".repeat(101), -1L, null, null)))
            .extracting(error -> error.getPropertyPath().toString()).containsExactlyInAnyOrder("buscar", "pisoId");
        assertThat(validador.validate(new FiltrosReporte.Ocupacion(0, -1L, "x".repeat(11), null)))
            .extracting(error -> error.getPropertyPath().toString()).containsExactlyInAnyOrder("cama", "pisoId");
    }
}
