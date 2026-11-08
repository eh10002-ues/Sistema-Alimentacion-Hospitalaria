package sv.edu.ues.alimentacion_hospitalaria.reportes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.TipoReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.web.EnlacesConsultaReportes;

class EnlacesConsultaReportesTests {
    private final EnlacesConsultaReportes enlaces = new EnlacesConsultaReportes();
    private final LocalDate fecha = LocalDate.of(2026, 11, 8);

    @Test
    void asignacionesConservaFechaEfectivaPisoDietaYBusqueda() {
        var pagina = new PageImpl<>(List.of("fila"), PageRequest.of(1, 20), 45);
        var resultado = enlaces.crear(TipoReporte.ASIGNACIONES,
            new FiltrosReporte.Asignaciones(1, "Ana + José", 5L, null, TipoDieta.BLANDA), fecha, pagina);
        assertThat(resultado.siguiente()).startsWith("/reportes/asignaciones?")
            .contains("buscar=Ana%20%2B%20Jos%C3%A9", "pisoId=5", "tipoDieta=BLANDA", "fecha=2026-11-08", "pagina=2");
        assertThat(resultado.anterior()).contains("pagina=0");
        assertThat(resultado.primera()).contains("pagina=0", "fecha=2026-11-08");
    }

    @Test
    void menusConservaFechaTiempoYTipoDeDieta() {
        var pagina = new PageImpl<>(List.of("fila"), PageRequest.of(0, 20), 45);
        var resultado = enlaces.crear(TipoReporte.MENUS,
            new FiltrosReporte.Menus(0, fecha, TiempoComida.ALMUERZO, TipoDieta.HIPOSODICA), fecha, pagina);
        assertThat(resultado.siguiente()).startsWith("/reportes/menus?")
            .contains("tiempoComida=ALMUERZO", "tipoDieta=HIPOSODICA", "fecha=2026-11-08", "pagina=1");
        assertThat(resultado.anterior()).isNull();
    }

    @Test
    void ocupacionConservaPisoCamaYEstado() {
        var pagina = new PageImpl<>(List.of("fila"), PageRequest.of(0, 20), 45);
        var resultado = enlaces.crear(TipoReporte.OCUPACION,
            new FiltrosReporte.Ocupacion(0, 4L, "C&1", FiltrosReporte.EstadoOcupacion.INCONSISTENTE), null, pagina);
        assertThat(resultado.siguiente()).startsWith("/reportes/ocupacion?")
            .contains("pisoId=4", "cama=C%261", "ocupacion=INCONSISTENTE", "pagina=1");
    }
}
