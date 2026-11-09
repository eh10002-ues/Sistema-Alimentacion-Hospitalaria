package sv.edu.ues.alimentacion_hospitalaria.reportes.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import sv.edu.ues.alimentacion_hospitalaria.asignaciones.domain.AsignacionMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.EstadoPrescripcion;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilaComanda;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.AsignacionPiso;

/**
 * SAH-9: consultas de solo lectura de los reportes específicos.
 * Los filtros de vigencia de RE-02 y RE-04 son los mismos de SAH-6 (SC-03.1) y de la proyección de insumos:
 * asignación activa y aprobada, paciente INGRESADO, menú ACTIVO y fecha dentro del período de la asignación.
 */
public interface ReportesRepository extends Repository<AsignacionMenu, Long> {

    @Query("select piso from Piso piso order by piso.codigo")
    List<Piso> buscarPisos();

    @Query("select piso from Piso piso where piso.id = :pisoId")
    Optional<Piso> buscarPiso(@Param("pisoId") Long pisoId);

    // RE-01: alergias de la severidad indicada, de pacientes en el estado indicado, por piso.
    @Query("""
        select alergia from PacienteAlergia alergia
        join fetch alergia.paciente paciente
        left join fetch paciente.cama cama
        left join fetch cama.piso piso
        where alergia.severidad = :severidad
          and paciente.estado = :estadoPaciente
          and (:pisoId is null or piso.id = :pisoId)
        order by piso.codigo nulls last, cama.numero, paciente.apellidos, paciente.nombres, paciente.codigo,
                 alergia.nombre, alergia.id
        """)
    List<PacienteAlergia> buscarAlergias(
        @Param("severidad") SeveridadAlergia severidad,
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("pisoId") Long pisoId);

    // RE-02: raciones por tiempo de comida, platillo, menú, régimen y piso de las asignaciones vigentes.
    // No se selecciona la hora de servicio (pendiente de la corrección de columnas TIME).
    @Query("""
        select new sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FilaComanda(
            detalle.tiempoComida, detalle.platillo, menu.id, menu.nombre, menu.tipoDieta,
            piso.codigo, piso.nombre, count(asignacion.id))
        from AsignacionMenu asignacion
        join asignacion.menu menu
        join menu.detalles detalle
        join asignacion.paciente paciente
        left join paciente.cama cama
        left join cama.piso piso
        where asignacion.activa = true
          and asignacion.aprobadoPor is not null
          and paciente.estado = :estadoPaciente
          and menu.estado = :estadoMenu
          and asignacion.fechaInicio <= :fecha
          and (asignacion.fechaFin is null or asignacion.fechaFin >= :fecha)
          and (:pisoId is null or piso.id = :pisoId)
          and (:tiempoComida is null or detalle.tiempoComida = :tiempoComida)
        group by detalle.tiempoComida, detalle.platillo, menu.id, menu.nombre, menu.tipoDieta,
                 piso.id, piso.codigo, piso.nombre
        """)
    List<FilaComanda> contarRaciones(
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("estadoMenu") EstadoMenu estadoMenu,
        @Param("fecha") LocalDate fecha,
        @Param("pisoId") Long pisoId,
        @Param("tiempoComida") TiempoComida tiempoComida);

    // RE-03: asignaciones de menú activas sin aprobar (no son prescripciones dietéticas).
    @Query("""
        select asignacion from AsignacionMenu asignacion
        join fetch asignacion.menu menu
        join fetch asignacion.asignadoPor
        join fetch asignacion.paciente paciente
        left join fetch paciente.cama cama
        left join fetch cama.piso piso
        where asignacion.activa = true
          and asignacion.aprobadoPor is null
          and paciente.estado = :estadoPaciente
        order by asignacion.createdAt, asignacion.id
        """)
    List<AsignacionMenu> buscarAsignacionesPendientes(@Param("estadoPaciente") EstadoPaciente estadoPaciente);

    // RE-04: pacientes sin una asignación vigente con los mismos filtros de SAH-6 en la fecha indicada.
    @Query("""
        select paciente from Paciente paciente
        left join fetch paciente.cama cama
        left join fetch cama.piso piso
        where paciente.estado = :estadoPaciente
          and (:pisoId is null or piso.id = :pisoId)
          and not exists (
              select asignacion.id from AsignacionMenu asignacion
              where asignacion.paciente = paciente
                and asignacion.activa = true
                and asignacion.aprobadoPor is not null
                and asignacion.menu.estado = :estadoMenu
                and asignacion.fechaInicio <= :fecha
                and (asignacion.fechaFin is null or asignacion.fechaFin >= :fecha))
        order by piso.codigo nulls last, cama.numero, paciente.apellidos, paciente.nombres, paciente.codigo
        """)
    List<Paciente> buscarPacientesSinMenuVigente(
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("estadoMenu") EstadoMenu estadoMenu,
        @Param("fecha") LocalDate fecha,
        @Param("pisoId") Long pisoId);

    // RE-04: pacientes que tienen al menos una asignación activa sin aprobar (mismo criterio de RE-03).
    @Query("""
        select distinct asignacion.paciente.id from AsignacionMenu asignacion
        where asignacion.paciente.id in :pacienteIds
          and asignacion.activa = true
          and asignacion.aprobadoPor is null
        """)
    List<Long> buscarPacientesConAsignacionPendiente(@Param("pacienteIds") Collection<Long> pacienteIds);

    // RE-04: asignaciones aprobadas de los pacientes, de la más reciente a la más antigua.
    @Query("""
        select asignacion from AsignacionMenu asignacion
        join fetch asignacion.menu
        where asignacion.paciente.id in :pacienteIds
          and asignacion.aprobadoPor is not null
        order by asignacion.fechaInicio desc, asignacion.id desc
        """)
    List<AsignacionMenu> buscarAsignacionesAprobadas(@Param("pacienteIds") Collection<Long> pacienteIds);

    @Query("""
        select paciente from Paciente paciente
        left join fetch paciente.cama cama
        left join fetch cama.piso piso
        where lower(paciente.codigo) = lower(:codigo)
        """)
    Optional<Paciente> buscarPacientePorCodigo(@Param("codigo") String codigo);

    @Query("""
        select asignacion from AsignacionMenu asignacion
        join fetch asignacion.menu menu
        where asignacion.paciente.id = :pacienteId
          and (:activa is null or asignacion.activa = :activa)
          and (:fechaDesde is null or asignacion.fechaFin is null or asignacion.fechaFin >= :fechaDesde)
          and (:fechaHasta is null or asignacion.fechaInicio <= :fechaHasta)
        order by asignacion.fechaInicio desc, asignacion.id desc
        """)
    List<AsignacionMenu> buscarAsignacionesPaciente(
        @Param("pacienteId") Long pacienteId,
        @Param("activa") Boolean activa,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta);

    @Query("""
        select prescripcion from PrescripcionDieta prescripcion
        join fetch prescripcion.paciente paciente
        left join fetch paciente.cama cama
        left join fetch cama.piso piso
        where paciente.estado = :estadoPaciente
          and prescripcion.estado = :estadoPrescripcion
          and (:pisoId is null or piso.id = :pisoId)
        order by piso.codigo nulls last, cama.numero, paciente.apellidos, paciente.nombres,
                 prescripcion.fechaAutorizacion desc, prescripcion.id desc
        """)
    List<PrescripcionDieta> buscarPrescripcionesActuales(
        @Param("estadoPaciente") EstadoPaciente estadoPaciente,
        @Param("estadoPrescripcion") sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.EstadoPrescripcion estadoPrescripcion,
        @Param("pisoId") Long pisoId);

    @Query("""
        select asignacion from AsignacionPiso asignacion
        join fetch asignacion.piso piso
        join fetch asignacion.usuario usuario
        where usuario.activo = true
          and (:pisoId is null or piso.id = :pisoId)
        order by piso.codigo, asignacion.turno
        """)
    List<AsignacionPiso> buscarEncargadosPisos(@Param("pisoId") Long pisoId);

    @Query("""
        select prescripcion from PrescripcionDieta prescripcion
        join fetch prescripcion.paciente paciente
        join fetch prescripcion.autorizadoPor autorizadoPor
        where prescripcion.paciente.id = :pacienteId
        order by prescripcion.fechaAutorizacion desc, prescripcion.id desc
        """)
    List<PrescripcionDieta> buscarHistorialPrescripciones(@Param("pacienteId") Long pacienteId);

    @Query("""
        select prescripcion from PrescripcionDieta prescripcion
        join fetch prescripcion.paciente paciente
        where prescripcion.paciente.id = :pacienteId
          and prescripcion.estado = :estadoPrescripcion
        order by prescripcion.fechaAutorizacion desc, prescripcion.id desc
        """)
    List<PrescripcionDieta> buscarPrescripcionesPaciente(
        @Param("pacienteId") Long pacienteId,
        @Param("estadoPrescripcion") EstadoPrescripcion estadoPrescripcion);
}
