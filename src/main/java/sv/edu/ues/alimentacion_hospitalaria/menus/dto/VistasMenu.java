package sv.edu.ues.alimentacion_hospitalaria.menus.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;

/** Modelos de vista del módulo de menús; se construyen dentro de la transacción del servicio. */
public final class VistasMenu {

    private VistasMenu() {
    }

    public record Resumen(Long id, String nombre, TipoMenu tipo, TipoDieta tipoDieta, EstadoMenu estado,
                          int tiemposComida) { }

    public record Menu(Long id, String nombre, TipoMenu tipo, TipoDieta tipoDieta, EstadoMenu estado,
                       List<Detalle> detalles) { }

    public record Detalle(Long id, TiempoComida tiempoComida, LocalTime horaServicio, String platillo,
                          List<Insumo> insumos) { }

    public record Insumo(Long id, String nombre, String unidad, BigDecimal cantidadPorRacion) { }

    public record OpcionInsumo(Long id, String nombre, String unidad) { }
}
