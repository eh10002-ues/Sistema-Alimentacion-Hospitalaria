package sv.edu.ues.alimentacion_hospitalaria.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence.AsignacionMenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.persistence.MenuRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

/** Datos sintéticos de los reportes de SAH-9. Cada piso tiene un código aleatorio para aislar los totales. */
record DatosReportes(PisoRepository pisos, CamaRepository camas, PacienteRepository pacientes, MenuRepository menus,
                     AsignacionMenuRepository asignaciones) {

    static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    Piso piso(String nombre) {
        return pisos.save(new Piso("R" + sufijo().substring(0, 5), nombre));
    }

    /** Paciente ingresado en una cama nueva del piso indicado; sin cama si el piso es nulo. */
    Paciente paciente(Piso piso, String nombres, String apellidos) {
        String sufijo = sufijo();
        Paciente paciente = new Paciente("REP-" + sufijo, nombres, apellidos, LocalDate.of(1985, 4, 12),
            new BigDecimal("65.00"));
        paciente.setAlturaCm(new BigDecimal("165.00"));
        if (piso != null) {
            paciente.setCama(camas.save(new Cama(piso, "C" + sufijo.substring(0, 4))));
        }
        return pacientes.save(paciente);
    }

    Paciente conAlergia(Paciente paciente, String nombre, SeveridadAlergia severidad) {
        paciente.agregarAlergia(new PacienteAlergia(nombre, severidad));
        return pacientes.save(paciente);
    }

    /** Menú con desayuno, almuerzo y cena; las horas se guardan desordenadas a propósito. */
    Menu menu(String nombre, TipoDieta tipoDieta, EstadoMenu estado) {
        Menu menu = new Menu(nombre + " " + sufijo(), TipoMenu.GENERAL, tipoDieta);
        menu.setEstado(estado);
        menu.agregarDetalle(new MenuDetalle(TiempoComida.CENA, LocalTime.of(5, 0), "Sopa " + menu.getNombre()));
        menu.agregarDetalle(new MenuDetalle(TiempoComida.DESAYUNO, LocalTime.of(21, 0), "Avena " + menu.getNombre()));
        menu.agregarDetalle(new MenuDetalle(TiempoComida.ALMUERZO, LocalTime.of(13, 0), "Pollo " + menu.getNombre()));
        return menus.save(menu);
    }

    AsignacionMenu aprobada(Paciente paciente, Menu menu, Usuario usuario, LocalDate inicio, LocalDate fin) {
        AsignacionMenu asignacion = new AsignacionMenu(paciente, menu, usuario, inicio);
        asignacion.setFechaFin(fin);
        asignacion.setAprobadoPor(usuario);
        asignacion.setFechaAprobacion(inicio);
        return asignaciones.save(asignacion);
    }

    AsignacionMenu pendiente(Paciente paciente, Menu menu, Usuario usuario, LocalDate inicio) {
        return asignaciones.save(new AsignacionMenu(paciente, menu, usuario, inicio));
    }
}
