package sv.edu.ues.alimentacion_hospitalaria.prescripciones.service;

import sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto.OrdenCocina;

/** Evento publicado dentro de la transacción de autorización; cocina lo recibe después del commit. */
public record PrescripcionAutorizada(OrdenCocina orden) {
}
