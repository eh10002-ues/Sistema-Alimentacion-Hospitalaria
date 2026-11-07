package sv.edu.ues.alimentacion_hospitalaria.ubicaciones.service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.AsignacionPiso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.AsignacionPisoForm;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.AsignacionPisoItemForm;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.CamaForm;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.EncargadoPisoVista;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.PisoDetalleVista;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.PisoForm;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.dto.PisoVista;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.AsignacionPisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdministracionUbicacionesService {

    private final PisoRepository pisos;
    private final CamaRepository camas;
    private final AsignacionPisoRepository asignaciones;
    private final UsuarioRepository usuarios;

    public AdministracionUbicacionesService(PisoRepository pisos, CamaRepository camas,
        AsignacionPisoRepository asignaciones, UsuarioRepository usuarios) {
        this.pisos = pisos;
        this.camas = camas;
        this.asignaciones = asignaciones;
        this.usuarios = usuarios;
    }

    public Page<PisoVista> listar(int pagina, String busqueda) {
        String filtro = busqueda == null ? "" : busqueda.trim();
        Pageable pageable = PageRequest.of(Math.max(0, pagina), 20, Sort.by("codigo", "id"));
        return pisos.buscar(filtro, pageable).map(piso -> new PisoVista(
            piso.getId(), piso.getCodigo(), piso.getNombre(),
            asignaciones.countByPisoId(piso.getId()), camas.countByPisoId(piso.getId())));
    }

    public Piso buscarPiso(Long id) {
        return pisos.findById(id).orElseThrow(() -> new ResourceNotFoundException("El piso solicitado no existe."));
    }

    public PisoForm consultarPiso(Long id) {
        Piso piso = buscarPiso(id);
        PisoForm formulario = new PisoForm();
        formulario.setCodigo(piso.getCodigo());
        formulario.setNombre(piso.getNombre());
        return formulario;
    }

    public PisoDetalleVista consultarDetalle(Long id) {
        Piso piso = buscarPiso(id);
        java.util.List<EncargadoPisoVista> encargados = asignaciones.findByPisoIdOrderByTurnoAsc(id).stream()
            .map(asignacion -> new EncargadoPisoVista(
                asignacion.getUsuario().getId(), asignacion.getUsuario().getNombreUsuario(),
                asignacion.getUsuario().getNombreCompleto(), asignacion.getTurno()))
            .toList();
        return new PisoDetalleVista(piso, encargados, camas.findByPisoIdOrderByNumeroAsc(id));
    }

    public java.util.List<Usuario> encargadosActivos() {
        return usuarios.findByRolAndActivoTrue(Rol.ROLE_ENCARGADO_PISO);
    }

    @Transactional
    public void crear(PisoForm formulario) {
        normalizar(formulario);
        comprobarCodigoDisponible(formulario.getCodigo(), null);
        comprobarNombreDisponible(formulario.getNombre(), null);
        pisos.save(new Piso(formulario.getCodigo(), formulario.getNombre().trim()));
    }

    @Transactional
    public void editar(Long id, PisoForm formulario) {
        normalizar(formulario);
        comprobarCodigoDisponible(formulario.getCodigo(), id);
        comprobarNombreDisponible(formulario.getNombre(), id);
        Piso piso = buscarPiso(id);
        piso.setCodigo(formulario.getCodigo());
        piso.setNombre(formulario.getNombre().trim());
    }

    @Transactional
    public void crearCamas(Long pisoId, CamaForm formulario) {
        if (formulario.estaVacio()) {
            throw new IllegalArgumentException("Agregue al menos una cama.");
        }
        Piso piso = buscarPiso(pisoId);
        Set<String> numeros = new HashSet<>();
        for (CamaForm.CodigoCama cama : formulario.getCamas()) {
            if (cama.getNumero() == null || cama.getNumero().isBlank()) {
                throw new IllegalArgumentException("Complete el código de cada cama.");
            }
            String numero = cama.getNumero().trim();
            if (!numeros.add(numero.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("No repita el código de una cama dentro del formulario.");
            }
            if (cama.getId() == null && camas.existsByPisoIdAndNumeroIgnoreCase(pisoId, numero)) {
                throw new IllegalArgumentException("Ya existe una cama con el código " + numero + " en el piso seleccionado.");
            }
            if (cama.getId() != null && camas.existsByPisoIdAndNumeroIgnoreCaseAndIdNot(pisoId, numero, cama.getId())) {
                throw new IllegalArgumentException("Ya existe una cama con el código " + numero + " en el piso seleccionado.");
            }
        }
        for (CamaForm.CodigoCama cama : formulario.getCamas()) {
            if (cama.getId() == null) {
                camas.save(new Cama(piso, cama.getNumero().trim()));
            } else {
                Cama existente = camas.findById(cama.getId())
                    .orElseThrow(() -> new IllegalArgumentException("La cama seleccionada ya no existe."));
                if (!existente.getPiso().getId().equals(pisoId)) {
                    throw new IllegalArgumentException("La cama no pertenece al piso seleccionado.");
                }
                existente.setNumero(cama.getNumero().trim());
            }
        }
    }

    public CamaForm consultarCamas(Long pisoId) {
        buscarPiso(pisoId);
        CamaForm formulario = new CamaForm();
        formulario.setCamas(camas.findByPisoIdOrderByNumeroAsc(pisoId).stream()
            .map(cama -> {
                CamaForm.CodigoCama codigo = new CamaForm.CodigoCama();
                codigo.setId(cama.getId());
                codigo.setNumero(cama.getNumero());
                return codigo;
            })
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        if (formulario.getCamas().isEmpty()) {
            formulario.getCamas().add(new CamaForm.CodigoCama());
        }
        return formulario;
    }

    @Transactional
    public void asignarEncargados(Long pisoId, AsignacionPisoForm formulario) {
        if (formulario.estaVacio()) {
            throw new IllegalArgumentException("Agregue al menos un encargado.");
        }
        Piso piso = buscarPiso(pisoId);
        Set<String> claves = new HashSet<>();
        Set<String> pisosPorTurno = new HashSet<>();
        for (AsignacionPisoItemForm item : formulario.getAsignaciones()) {
            if (item.getUsuarioId() == null || item.getTurno() == null) {
                throw new IllegalArgumentException("Complete el encargado y el turno de cada asignación.");
            }
            String clave = item.getUsuarioId() + ":" + item.getTurno();
            if (!claves.add(clave)) {
                throw new IllegalArgumentException("No repita el mismo encargado en el mismo turno.");
            }
            if (!pisosPorTurno.add(item.getTurno().name())) {
                throw new IllegalArgumentException("El piso solo puede tener un encargado por turno.");
            }
            Usuario usuario = usuarios.findById(item.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("El usuario seleccionado no existe."));
            if (!usuario.isActivo() || usuario.getRol() != Rol.ROLE_ENCARGADO_PISO) {
                throw new IllegalArgumentException("El usuario seleccionado debe tener el rol Encargado de piso y estar activo.");
            }
            if (asignaciones.existsByUsuarioIdAndTurno(usuario.getId(), item.getTurno())) {
                throw new IllegalArgumentException("El usuario ya tiene asignado un piso en ese turno.");
            }
            if (asignaciones.findByPisoIdAndTurno(pisoId, item.getTurno()).isPresent()) {
                throw new IllegalArgumentException("El piso ya tiene un encargado para ese turno.");
            }
        }
        for (AsignacionPisoItemForm item : formulario.getAsignaciones()) {
            Usuario usuario = usuarios.findById(item.getUsuarioId()).orElseThrow();
            asignaciones.save(new AsignacionPiso(piso, usuario, item.getTurno()));
        }
    }

    private void normalizar(PisoForm formulario) {
        formulario.setCodigo(formulario.getCodigo().trim().toUpperCase(Locale.ROOT));
        formulario.setNombre(formulario.getNombre().trim());
    }

    private void comprobarCodigoDisponible(String codigo, Long id) {
        if (id == null && pisos.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException("Ya existe un piso con ese código.");
        }
        if (id != null && pisos.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new IllegalArgumentException("Ya existe un piso con ese código.");
        }
    }

    private void comprobarNombreDisponible(String nombre, Long id) {
        if (id == null && pisos.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("Ya existe un piso con ese nombre.");
        }
        if (id != null && pisos.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new IllegalArgumentException("Ya existe un piso con ese nombre.");
        }
    }
}
