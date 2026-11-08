package sv.edu.ues.alimentacion_hospitalaria.reportes.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.TipoReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.AccesoReportes;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.CatalogoReportesService;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.ConsultaReportesService;
import sv.edu.ues.alimentacion_hospitalaria.reportes.service.FiltroReporteInvalidoException;
import sv.edu.ues.alimentacion_hospitalaria.reportes.exception.LimiteResultadosException;

/** HU-02: reportes generales, filtros e impresión; independiente de los específicos SAH-9. */
@Controller
@RequestMapping("/reportes")
public class ReportesGeneralesController {
    private final CatalogoReportesService catalogo;
    private final ConsultaReportesService consultas;
    private final AccesoReportes acceso;
    private final EnlacesConsultaReportes enlaces;
    private final ResumenFiltrosReportes resumen;

    public ReportesGeneralesController(CatalogoReportesService catalogo, ConsultaReportesService consultas,
                              AccesoReportes acceso, EnlacesConsultaReportes enlaces, ResumenFiltrosReportes resumen) {
        this.catalogo = catalogo;
        this.consultas = consultas;
        this.acceso = acceso;
        this.enlaces = enlaces;
        this.resumen = resumen;
    }

    @ModelAttribute
    void comprobarRol() {
        // Mantiene el contrato de un solo rol incluso antes del enlace de filtros.
        acceso.rolActual();
    }

    @GetMapping({"", "/"})
    String inicio(Model model) {
        model.addAttribute("reportes", catalogo.consultar());
        return "reportes/inicio";
    }

    @GetMapping({"/pacientes", "/pacientes/"})
    String pacientes(@Valid @ModelAttribute("filtros") FiltrosReporte.Pacientes filtros, Model model) {
        return preparar(TipoReporte.PACIENTES, consultas.pacientes(filtros), model);
    }

    @GetMapping({"/asignaciones", "/asignaciones/"})
    String asignaciones(@Valid @ModelAttribute("filtros") FiltrosReporte.Asignaciones filtros, Model model) {
        var resultado = consultas.asignaciones(filtros);
        model.addAttribute("fechaConsulta", resultado.fechaConsulta());
        return preparar(TipoReporte.ASIGNACIONES, resultado.pagina(), model);
    }

    @GetMapping({"/menus", "/menus/"})
    String menus(@Valid @ModelAttribute("filtros") FiltrosReporte.Menus filtros, Model model) {
        var resultado = consultas.menus(filtros);
        model.addAttribute("fechaConsulta", resultado.fechaConsulta());
        return preparar(TipoReporte.MENUS, resultado.pagina(), model);
    }

    @GetMapping({"/ocupacion", "/ocupacion/"})
    String ocupacion(@Valid @ModelAttribute("filtros") FiltrosReporte.Ocupacion filtros, Model model) {
        return preparar(TipoReporte.OCUPACION, consultas.ocupacion(filtros), model);
    }

    @GetMapping({"/pacientes/impresion", "/pacientes/impresion/"})
    String pacientesImpresion(@Valid @ModelAttribute("filtros") FiltrosReporte.Pacientes filtros, Model model) {
        return preparar(TipoReporte.PACIENTES, consultas.pacientesImpresion(filtros), model, true);
    }

    @GetMapping({"/asignaciones/impresion", "/asignaciones/impresion/"})
    String asignacionesImpresion(@Valid @ModelAttribute("filtros") FiltrosReporte.Asignaciones filtros, Model model) {
        var resultado = consultas.asignacionesImpresion(filtros);
        model.addAttribute("fechaConsulta", resultado.fechaConsulta());
        return preparar(TipoReporte.ASIGNACIONES, resultado.pagina(), model, true);
    }

    @GetMapping({"/menus/impresion", "/menus/impresion/"})
    String menusImpresion(@Valid @ModelAttribute("filtros") FiltrosReporte.Menus filtros, Model model) {
        var resultado = consultas.menusImpresion(filtros);
        model.addAttribute("fechaConsulta", resultado.fechaConsulta());
        return preparar(TipoReporte.MENUS, resultado.pagina(), model, true);
    }

    @GetMapping({"/ocupacion/impresion", "/ocupacion/impresion/"})
    String ocupacionImpresion(@Valid @ModelAttribute("filtros") FiltrosReporte.Ocupacion filtros, Model model) {
        return preparar(TipoReporte.OCUPACION, consultas.ocupacionImpresion(filtros), model, true);
    }

    private String preparar(TipoReporte tipo, Page<?> pagina, Model model) {
        return preparar(tipo, pagina, model, false);
    }

    private String preparar(TipoReporte tipo, Page<?> pagina, Model model, boolean impresion) {
        model.addAttribute("tipoReporte", tipo);
        model.addAttribute("titulo", tipo.getTitulo());
        model.addAttribute("pagina", pagina);
        model.addAttribute("enlaces", enlaces.crear(tipo, model.getAttribute("filtros"),
            (java.time.LocalDate) model.getAttribute("fechaConsulta"), pagina));
        if (!impresion) {
            if (tipo != TipoReporte.MENUS) { model.addAttribute("pisos", catalogo.pisos()); }
            model.addAttribute("estadosPaciente", sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente.values());
            model.addAttribute("tiposDieta", sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta.values());
            model.addAttribute("tiemposComida", sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida.values());
            model.addAttribute("estadosOcupacion", FiltrosReporte.EstadoOcupacion.values());
        }
        if (tipo == TipoReporte.ASIGNACIONES) {
            var rol = acceso.rolActual();
            boolean cocina = rol == sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol.ROLE_COCINA;
            boolean piso = rol == sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol.ROLE_ENCARGADO_PISO;
            model.addAttribute("mostrarFechasAsignacion", !cocina && !piso);
            model.addAttribute("mostrarEstadoAsignacion", !cocina);
            model.addAttribute("mostrarResponsable", !cocina && !piso);
            model.addAttribute("mostrarTiempos", !piso);
            model.addAttribute("mostrarFechaServicio", cocina);
            model.addAttribute("columnasAsignaciones", cocina ? 8 : piso ? 7 : 11);
        }
        if (impresion) {
            model.addAttribute("generadoEn", java.time.ZonedDateTime.now(java.time.ZoneId.of("America/El_Salvador")));
            model.addAttribute("resumenFiltros", resumen.crear(model.getAttribute("filtros"),
                (java.time.LocalDate) model.getAttribute("fechaConsulta")));
            return "reportes/impresion";
        }
        return "reportes/consulta";
    }

    @ExceptionHandler(LimiteResultadosException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    String limiteImpresion(LimiteResultadosException error, Model model) {
        model.addAttribute("total", error.getTotal());
        model.addAttribute("limite", error.getLimite());
        return "reportes/limite-impresion";
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String filtrosInvalidos(BindException error, Model model) {
        var mensajes = error.getBindingResult().getAllErrors().stream().map(item -> {
            if (item instanceof FieldError campo && campo.isBindingFailure()) {
                return switch (campo.getField()) {
                    case "pagina" -> "La página debe ser un número entero desde cero.";
                    case "pisoId" -> "Seleccione un piso válido.";
                    case "fecha" -> "Indique una fecha válida con formato aaaa-mm-dd.";
                    case "estado", "ocupacion" -> "Seleccione un estado válido.";
                    case "tipoDieta" -> "Seleccione un tipo de dieta válido.";
                    case "tiempoComida" -> "Seleccione un tiempo de comida válido.";
                    default -> "Revise los filtros del reporte.";
                };
            }
            return item.getDefaultMessage();
        }).distinct().toList();
        model.addAttribute("errores", mensajes);
        return "reportes/filtros-invalidos";
    }

    @ExceptionHandler(FiltroReporteInvalidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String filtroInvalido(FiltroReporteInvalidoException error, Model model) {
        model.addAttribute("errores", java.util.List.of(error.getMessage()));
        return "reportes/filtros-invalidos";
    }
}
