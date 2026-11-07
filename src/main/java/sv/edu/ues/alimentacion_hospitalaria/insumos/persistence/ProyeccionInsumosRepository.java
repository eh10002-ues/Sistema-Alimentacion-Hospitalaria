package sv.edu.ues.alimentacion_hospitalaria.insumos.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaDemandaProyeccion;
import sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaProyeccionInsumo;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;

public interface ProyeccionInsumosRepository extends Repository<AsignacionMenu, Long> {

    @Query("""
        select new sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaProyeccionInsumo(
            insumo.id, asignacion.id, paciente.id, insumo.nombre, insumo.unidad,
            ingrediente.cantidadPorRacion, insumo.merma, insumo.presentacion,
            insumo.cantidadPresentacion, insumo.costoUnitario,
            asignacion.fechaInicio, asignacion.fechaFin
        )
        from AsignacionMenu asignacion
        join asignacion.paciente paciente
        join asignacion.menu menu
        join menu.detalles detalle
        join detalle.insumos ingrediente
        join ingrediente.insumo insumo
        where asignacion.activa = true
          and asignacion.aprobadoPor is not null
          and paciente.estado = :estadoPaciente
          and menu.estado = :estadoMenu
          and asignacion.fechaInicio <= :fechaFin
          and (asignacion.fechaFin is null or asignacion.fechaFin >= :fechaInicio)
        order by insumo.nombre
        """)
    List<FilaProyeccionInsumo> buscarFilasVigentes(
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin,
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("estadoMenu") EstadoMenu estadoMenu);

    @Query("""
        select new sv.edu.ues.alimentacion_hospitalaria.insumos.dto.FilaDemandaProyeccion(
            asignacion.id, paciente.id, asignacion.fechaInicio, asignacion.fechaFin
        )
        from AsignacionMenu asignacion
        join asignacion.paciente paciente
        join asignacion.menu menu
        where asignacion.activa = true
          and asignacion.aprobadoPor is not null
          and paciente.estado = :estadoPaciente
          and menu.estado = :estadoMenu
          and asignacion.fechaInicio <= :fechaFin
          and (asignacion.fechaFin is null or asignacion.fechaFin >= :fechaInicio)
        """)
    List<FilaDemandaProyeccion> buscarDemandaVigente(
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin,
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("estadoMenu") EstadoMenu estadoMenu);
}
