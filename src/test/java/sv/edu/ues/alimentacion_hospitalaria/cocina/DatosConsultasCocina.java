package sv.edu.ues.alimentacion_hospitalaria.cocina;

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
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.DatosClinicosPrueba;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;

/** Datos sintéticos para las consultas de SAH-6; reutiliza los pacientes de prueba de SAH-2. */
record DatosConsultasCocina(DatosClinicosPrueba clinicos, PacienteRepository pacientes, MenuRepository menus,
                            AsignacionMenuRepository asignaciones) {

    Paciente pacienteIngresado(String nombres, String apellidos) {
        Paciente paciente = clinicos.pacienteIngresado();
        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        return pacientes.save(paciente);
    }

    Menu menu(String nombre, TipoDieta tipoDieta, EstadoMenu estado) {
        Menu menu = new Menu(nombre + " " + UUID.randomUUID().toString().substring(0, 6), TipoMenu.GENERAL, tipoDieta);
        menu.setEstado(estado);
        menu.agregarDetalle(new MenuDetalle(TiempoComida.CENA, LocalTime.of(18, 0), "Sopa de verduras"));
        menu.agregarDetalle(new MenuDetalle(TiempoComida.DESAYUNO, LocalTime.of(7, 0), "Avena con frutas"));
        menu.agregarDetalle(new MenuDetalle(TiempoComida.ALMUERZO, LocalTime.of(12, 30), "Pollo a la plancha"));
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
