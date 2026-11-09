package sv.edu.ues.alimentacion_hospitalaria.asignaciones.service;

import sv.edu.ues.alimentacion_hospitalaria.asignaciones.dto.OrdenMenuCocina;

/** Evento publicado al aprobar una asignación vigente; cocina lo recibe después del commit. */
public record AsignacionMenuAprobada(OrdenMenuCocina orden) {
}
