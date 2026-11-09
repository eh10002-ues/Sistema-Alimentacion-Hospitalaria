package sv.edu.ues.alimentacion_hospitalaria.asignaciones.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;

public interface AsignacionMenuRepository extends JpaRepository<AsignacionMenu, Long> {

    // Menús de un paciente
    List<AsignacionMenu> findByPacienteIdOrderByFechaInicioDesc(Long pacienteId);

    List<AsignacionMenu> findByPacienteIdAndActivaTrue(Long pacienteId);

    // Pacientes de un menú
    List<AsignacionMenu> findByMenuIdAndActivaTrue(Long menuId);

    // Asignaciones sin aprobar
    List<AsignacionMenu> findByAprobadoPorIsNull();

    // D4: asignaciones activas de pacientes ingresados; con soloPendientes, únicamente las no aprobadas.
    @Query("""
        select asignacion from AsignacionMenu asignacion
        join fetch asignacion.menu
        join fetch asignacion.paciente paciente
        join fetch asignacion.asignadoPor
        left join fetch asignacion.aprobadoPor aprobador
        left join fetch paciente.cama cama
        left join fetch cama.piso
        where asignacion.activa = true
          and paciente.estado = :estadoPaciente
          and (:soloPendientes = false or aprobador is null)
        order by asignacion.fechaInicio desc, asignacion.id desc
        """)
    List<AsignacionMenu> findActivasParaAdministracion(
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("soloPendientes") boolean soloPendientes);

    // D4: otra asignación activa (pendiente o aprobada) del paciente que se cruce con el período indicado.
    @Query("""
        select count(asignacion) > 0 from AsignacionMenu asignacion
        where asignacion.paciente.id = :pacienteId
          and asignacion.activa = true
          and asignacion.fechaInicio <= :fin
          and (asignacion.fechaFin is null or asignacion.fechaFin >= :inicio)
        """)
    boolean existeActivaEnPeriodo(@Param("pacienteId") Long pacienteId,
                                  @Param("inicio") LocalDate inicio,
                                  @Param("fin") LocalDate fin);

    // D4: órdenes de menú para cocina; mismos filtros de vigencia que SAH-6, sin filtrar por régimen.
    @Query("""
        select asignacion from AsignacionMenu asignacion
        join fetch asignacion.menu menu
        join fetch asignacion.paciente paciente
        left join fetch paciente.cama cama
        left join fetch cama.piso piso
        where menu.estado = :estadoMenu
          and asignacion.activa = true
          and asignacion.aprobadoPor is not null
          and paciente.estado = :estadoPaciente
          and asignacion.fechaInicio <= :fecha
          and (asignacion.fechaFin is null or asignacion.fechaFin >= :fecha)
        order by piso.codigo, cama.numero, paciente.apellidos, paciente.nombres, asignacion.id
        """)
    List<AsignacionMenu> findAprobadasVigentes(
        @Param("estadoMenu") EstadoMenu estadoMenu,
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("fecha") LocalDate fecha);

    // SAH-6 (SC-03.1): pacientes internos con un menú vigente del régimen indicado, en la fecha dada.
    // Incluye todos los menús del mismo TipoDieta y aplica los filtros de estado de la proyección de insumos.
    @Query("""
        select asignacion from AsignacionMenu asignacion
        join fetch asignacion.menu menu
        join fetch asignacion.paciente paciente
        left join fetch paciente.cama cama
        left join fetch cama.piso piso
        where menu.tipoDieta = :tipoDieta
          and menu.estado = :estadoMenu
          and asignacion.activa = true
          and asignacion.aprobadoPor is not null
          and paciente.estado = :estadoPaciente
          and asignacion.fechaInicio <= :fecha
          and (asignacion.fechaFin is null or asignacion.fechaFin >= :fecha)
        order by piso.codigo, cama.numero, paciente.apellidos, paciente.nombres, menu.nombre, asignacion.id
        """)
    List<AsignacionMenu> findVigentesPorTipoDieta(
        @Param("tipoDieta") TipoDieta tipoDieta,
        @Param("estadoMenu") EstadoMenu estadoMenu,
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("fecha") LocalDate fecha);

    // SAH-6 (SC-03.2): historial de menús aprobados del paciente, del más reciente al más antiguo.
    @Query("""
        select asignacion from AsignacionMenu asignacion
        join fetch asignacion.menu
        where asignacion.paciente.id = :pacienteId
          and asignacion.aprobadoPor is not null
        order by asignacion.fechaInicio desc, asignacion.id desc
        """)
    List<AsignacionMenu> findHistorialAprobadoPorPaciente(@Param("pacienteId") Long pacienteId);
}
