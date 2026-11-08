package sv.edu.ues.alimentacion_hospitalaria.reportes.dto;

import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;

/** RE-02: raciones agrupadas que devuelve la consulta. El piso es nulo para pacientes sin cama. */
public record FilaComanda(TiempoComida tiempoComida, String platillo, Long menuId, String menu, TipoDieta tipoDieta,
                          String pisoCodigo, String pisoNombre, Long raciones) {
}
