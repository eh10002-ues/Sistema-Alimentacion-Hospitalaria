package sv.edu.ues.alimentacion_hospitalaria.pacientes.service;

import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteDetalleVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteAlergiaRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteEnfermedadRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteMedicamentoRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class AdministracionPacientesService {

    private final PacienteRepository pacienteRepository;
    private final PacienteEnfermedadRepository enfermedadRepository;
    private final PacienteAlergiaRepository alergiaRepository;
    private final PacienteMedicamentoRepository medicamentoRepository;
    private final PisoRepository pisoRepository;
    private final CamaRepository camaRepository;
    private final UsuarioRepository usuarioRepository;

    public AdministracionPacientesService(PacienteRepository pacienteRepository,
                                          PacienteEnfermedadRepository enfermedadRepository,
                                          PacienteAlergiaRepository alergiaRepository,
                                          PacienteMedicamentoRepository medicamentoRepository,
                                          PisoRepository pisoRepository,
                                          CamaRepository camaRepository,
                                          UsuarioRepository usuarioRepository) {
        this.pacienteRepository = pacienteRepository;
        this.enfermedadRepository = enfermedadRepository;
        this.alergiaRepository = alergiaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.pisoRepository = pisoRepository;
        this.camaRepository = camaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Page<PacienteVista> listar(int pagina, String buscar, EstadoPaciente estado, Long pisoId) {
        String filtro = buscar == null ? "" : buscar.trim();
        Pageable pageable = PageRequest.of(Math.max(0, pagina), 20,
            Sort.by(Sort.Order.asc("codigo"), Sort.Order.asc("id")));
        return pacienteRepository.buscar(filtro, estado, pisoId, pageable).map(PacienteVista::desde);
    }

    public PacienteDetalleVista consultar(Long id) {
        var paciente = pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        paciente.getEnfermedades().size();
        paciente.getAlergias().size();
        paciente.getMedicamentos().size();
        return PacienteDetalleVista.desde(paciente);
    }

    public java.util.List<Piso> pisosDisponibles() {
        return pisoRepository.findAll(Sort.by(Sort.Order.asc("codigo"), Sort.Order.asc("id")))
            .stream()
            .filter(piso -> !camaRepository.buscarDisponibles(piso.getId(), EstadoPaciente.INGRESADO).isEmpty())
            .toList();
    }

    public java.util.List<Piso> pisos() {
        return pisoRepository.findAll(Sort.by(Sort.Order.asc("codigo"), Sort.Order.asc("id")));
    }

    public java.util.List<Cama> camasDisponibles(Long pisoId) {
        if (pisoId == null) {
            return java.util.List.of();
        }
        return camaRepository.buscarDisponibles(pisoId, EstadoPaciente.INGRESADO);
    }

    public java.util.List<Usuario> responsablesActivos() {
        var medicos = usuarioRepository.findByRolAndActivoTrue(Rol.ROLE_MEDICO);
        var nutricionistas = usuarioRepository.findByRolAndActivoTrue(Rol.ROLE_NUTRICIONISTA);
        var responsables = new java.util.ArrayList<Usuario>();
        responsables.addAll(medicos);
        responsables.addAll(nutricionistas);
        responsables.sort(java.util.Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER));
        return responsables;
    }

    @Transactional
    public Paciente crear(PacienteForm formulario) {
        String codigo = formulario.getCodigo().trim();
        if (pacienteRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException("Ya existe un paciente con ese código.");
        }

        Cama cama = camaRepository.findById(formulario.getCamaId())
            .orElseThrow(() -> new IllegalArgumentException("La cama seleccionada ya no existe."));
        if (!cama.getPiso().getId().equals(formulario.getPisoId())) {
            throw new IllegalArgumentException("La cama seleccionada no pertenece al piso indicado.");
        }

        pacienteRepository.findByCamaId(cama.getId()).ifPresent(pacienteExistente -> {
            if (pacienteExistente.getEstado() == EstadoPaciente.INGRESADO) {
                throw new IllegalArgumentException("La cama seleccionada ya está ocupada por otro paciente ingresado.");
            }
            pacienteExistente.setCama(null);
        });

        Usuario responsable = usuarioRepository.findById(formulario.getMedicoResponsableId())
            .orElseThrow(() -> new IllegalArgumentException("El médico responsable seleccionado ya no existe."));
        if (!responsable.isActivo()
            || (responsable.getRol() != Rol.ROLE_MEDICO && responsable.getRol() != Rol.ROLE_NUTRICIONISTA)) {
            throw new IllegalArgumentException("Seleccione un médico o nutricionista responsable activo.");
        }

        Paciente paciente = new Paciente(
            codigo.toUpperCase(Locale.ROOT),
            formulario.getNombres().trim(),
            formulario.getApellidos().trim(),
            formulario.getFechaNacimiento(),
            formulario.getPesoKg());
        paciente.setAlturaCm(formulario.getAlturaCm());
        paciente.setEstado(EstadoPaciente.INGRESADO);
        paciente.setCama(cama);
        paciente.setMedicoResponsable(responsable);
        return pacienteRepository.save(paciente);
    }
}
