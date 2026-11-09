package sv.edu.ues.alimentacion_hospitalaria.reportes.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.reportes.dto.FiltrosReporte;
import sv.edu.ues.alimentacion_hospitalaria.reportes.exception.LimiteResultadosException;

/** Consultas HU-02 de columnas escalares. Las filas paginadas nunca hacen fetch de colecciones. */
@Repository
@Transactional(readOnly = true)
public class ConsultasReportesRepository {
    private static final String BUSQUEDA_PACIENTE = """
        (:buscar = '' or lower(p.codigo) like lower(:buscarLike) escape '!'
            or lower(concat(p.nombres, ' ', p.apellidos)) like lower(:buscarLike) escape '!')
        """;
    private static final String VIGENCIA = """
        a.activa = true and a.aprobadoPor is not null and a.fechaAprobacion is not null
        and a.fechaInicio <= :fecha and (a.fechaFin is null or a.fechaFin >= :fecha)
        and p.estado = sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente.INGRESADO
        and m.estado = sv.edu.ues.alimentacion_hospitalaria.menus.domain.EstadoMenu.ACTIVO
        """;
    private static final String CONTEO_OCUPANTES = """
        (select count(p.id) from Paciente p where p.cama.id = c.id
          and p.estado = sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente.INGRESADO)
        """;
    private final EntityManager em;

    public ConsultasReportesRepository(EntityManager em) { this.em = em; }

    public List<Tuple> pisos() {
        return em.createQuery("select piso.id, piso.codigo, piso.nombre from Piso piso order by piso.codigo, piso.id", Tuple.class)
            .getResultList();
    }

    public Page<Tuple> pacientes(FiltrosReporte.Pacientes filtro, Pageable pagina, Integer limiteTotal) {
        String desde = """
            from Paciente p left join p.cama c left join c.piso piso
            where (:pisoId is null or piso.id = :pisoId) and (:estado is null or p.estado = :estado)
            and
            """ + BUSQUEDA_PACIENTE;
        var parametros = parametrosPaciente(filtro.buscar(), filtro.pisoId());
        parametros.put("estado", filtro.estado());
        return consultarPagina("p.codigo, concat(p.nombres, ' ', p.apellidos), piso.codigo, c.numero, p.estado",
            "p.id", desde, "p.codigo, p.id", parametros, pagina, limiteTotal);
    }

    public Page<Tuple> asignaciones(FiltrosReporte.Asignaciones filtro, LocalDate fecha, Pageable pagina, Integer limiteTotal) {
        String desde = """
            from AsignacionMenu a join a.paciente p join a.menu m join a.asignadoPor responsable
            left join p.cama c left join c.piso piso
            where (:pisoId is null or piso.id = :pisoId) and (:dieta is null or m.tipoDieta = :dieta)
            and
            """ + BUSQUEDA_PACIENTE + " and " + VIGENCIA;
        var parametros = parametrosPaciente(filtro.buscar(), filtro.pisoId());
        parametros.put("dieta", filtro.tipoDieta());
        parametros.put("fecha", fecha);
        return consultarPagina("""
            a.id, p.codigo, concat(p.nombres, ' ', p.apellidos), piso.codigo, c.numero,
            m.tipoDieta, m.nombre, a.fechaInicio, a.fechaFin, responsable.nombreCompleto, m.id
            """, "a.id", desde, "p.codigo, a.id", parametros, pagina, limiteTotal);
    }

    public List<Tuple> tiempos(List<Long> menus) {
        if (menus.isEmpty()) { return List.of(); }
        return parametros(em.createQuery("""
            select d.menu.id, d.tiempoComida, d.horaServicio from MenuDetalle d
            where d.menu.id in :menus
            """, Tuple.class), Map.of("menus", menus)).getResultList();
    }

    public Page<Tuple> menus(FiltrosReporte.Menus filtro, LocalDate fecha, Pageable pagina, Integer limiteTotal) {
        String desde = """
            from MenuDetalle d join d.menu m
            where (:dieta is null or m.tipoDieta = :dieta)
            and (:tiempo is null or d.tiempoComida = :tiempo)
            and exists (select a.id from AsignacionMenu a join a.paciente p where a.menu = m and
            """ + VIGENCIA + ")";
        var parametros = new java.util.HashMap<String, Object>();
        parametros.put("dieta", filtro.tipoDieta());
        parametros.put("tiempo", filtro.tiempoComida());
        parametros.put("fecha", fecha);
        // Orden operativo del día independiente del desplazamiento JDBC de LocalTime bajo UTC.
        String orden = """
            case d.tiempoComida
              when sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida.DESAYUNO then 0
              when sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida.COLACION_MATUTINA then 1
              when sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida.ALMUERZO then 2
              when sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida.COLACION_VESPERTINA then 3
              when sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida.CENA then 4
              else 5 end, m.nombre, m.id, d.id
            """;
        return consultarPagina("d.tiempoComida, d.horaServicio, m.nombre, m.tipoDieta, m.estado",
            "d.id", desde, orden, parametros, pagina, limiteTotal);
    }

    public Page<Tuple> ocupacion(FiltrosReporte.Ocupacion filtro, Pageable pagina, Integer limiteTotal) {
        String condicion = switch (filtro.ocupacion()) {
            case DISPONIBLE -> CONTEO_OCUPANTES + " = 0";
            case OCUPADA -> CONTEO_OCUPANTES + " = 1";
            case INCONSISTENTE -> CONTEO_OCUPANTES + " > 1";
            case null -> "1 = 1";
        };
        String desde = """
            from Cama c join c.piso piso where (:pisoId is null or piso.id = :pisoId)
            and (:cama = '' or lower(c.numero) like lower(:camaLike) escape '!') and
            """ + condicion;
        var parametros = new java.util.HashMap<String, Object>();
        parametros.put("pisoId", filtro.pisoId());
        parametros.put("cama", filtro.cama());
        parametros.put("camaLike", contiene(filtro.cama()));
        return consultarPagina("c.id, piso.codigo, c.numero, " + CONTEO_OCUPANTES, "c.id", desde,
            "piso.codigo, piso.id, c.numero, c.id", parametros, pagina, limiteTotal);
    }

    public List<Tuple> ocupantes(List<Long> camas) {
        if (camas.isEmpty()) { return List.of(); }
        return parametros(em.createQuery("""
            select p.cama.id, p.codigo, concat(p.nombres, ' ', p.apellidos) from Paciente p
            where p.cama.id in :camas
            and p.estado = sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente.INGRESADO
            order by p.codigo, p.id
            """, Tuple.class), Map.of("camas", camas)).getResultList();
    }

    private Page<Tuple> consultarPagina(String columnas, String id, String desde, String orden,
                                        Map<String, Object> valores, Pageable pagina, Integer limiteTotal) {
        long total = parametros(em.createQuery("select count(" + id + ") " + desde, Long.class), valores)
            .getSingleResult();
        if (limiteTotal != null && total > limiteTotal) {
            throw new LimiteResultadosException(total, limiteTotal);
        }
        if (pagina.getOffset() >= total) { return new PageImpl<>(List.of(), pagina, total); }
        List<Tuple> filas = parametros(em.createQuery("select " + columnas + " " + desde + " order by " + orden,
            Tuple.class), valores).setFirstResult(Math.toIntExact(pagina.getOffset()))
            .setMaxResults(pagina.getPageSize()).getResultList();
        return new PageImpl<>(filas, pagina, total);
    }

    private Map<String, Object> parametrosPaciente(String buscar, Long pisoId) {
        var parametros = new java.util.HashMap<String, Object>();
        parametros.put("buscar", buscar);
        parametros.put("buscarLike", contiene(buscar));
        parametros.put("pisoId", pisoId);
        return parametros;
    }

    private String contiene(String texto) {
        return "%" + texto.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private <T> TypedQuery<T> parametros(TypedQuery<T> query, Map<String, ?> parametros) {
        parametros.forEach(query::setParameter);
        return query;
    }
}
