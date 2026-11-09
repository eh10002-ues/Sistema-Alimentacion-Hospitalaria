package sv.edu.ues.alimentacion_hospitalaria.pacientes.service;

import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.service.RegistroBitacoraService;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteDetalleVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.AlergiaForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.EnfermedadForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.MedicamentoForm;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteEnfermedad;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteMedicamento;
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
    private final RegistroBitacoraService bitacora;

    public AdministracionPacientesService(PacienteRepository pacienteRepository,
                                          PacienteEnfermedadRepository enfermedadRepository,
                                          PacienteAlergiaRepository alergiaRepository,
                                          PacienteMedicamentoRepository medicamentoRepository,
                                          PisoRepository pisoRepository,
                                          CamaRepository camaRepository,
                                          UsuarioRepository usuarioRepository,
                                          RegistroBitacoraService bitacora) {
        this.pacienteRepository = pacienteRepository;
        this.enfermedadRepository = enfermedadRepository;
        this.alergiaRepository = alergiaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.pisoRepository = pisoRepository;
        this.camaRepository = camaRepository;
        this.usuarioRepository = usuarioRepository;
        this.bitacora = bitacora;
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

    public Paciente consultarPacienteClinico(Long id) {
        return pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
    }

    public java.util.List<PacienteEnfermedad> enfermedades(Long pacienteId) {
        return enfermedadRepository.findByPacienteId(pacienteId);
    }

    @Transactional
    public void agregarEnfermedad(Long pacienteId, EnfermedadForm formulario) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        paciente.agregarEnfermedad(new PacienteEnfermedad(formulario.getNombre().trim()));
        pacienteRepository.save(paciente);
        actualizarLista(pacienteId, "enfermedades");
    }

    @Transactional
    public void actualizarEnfermedad(Long pacienteId, Long enfermedadId, EnfermedadForm formulario) {
        PacienteEnfermedad enfermedad = enfermedadRepository.findById(enfermedadId)
            .orElseThrow(() -> new ResourceNotFoundException("La enfermedad solicitada no existe."));
        if (!enfermedad.getPaciente().getId().equals(pacienteId)) {
            throw new IllegalArgumentException("La enfermedad no pertenece al paciente seleccionado.");
        }
        enfermedad.setNombre(formulario.getNombre().trim());
        enfermedadRepository.save(enfermedad);
        actualizarLista(pacienteId, "enfermedades");
    }

    @Transactional
    public void eliminarEnfermedad(Long pacienteId, Long enfermedadId) {
        PacienteEnfermedad enfermedad = enfermedadRepository.findById(enfermedadId)
            .orElseThrow(() -> new ResourceNotFoundException("La enfermedad solicitada no existe."));
        if (!enfermedad.getPaciente().getId().equals(pacienteId)) {
            throw new IllegalArgumentException("La enfermedad no pertenece al paciente seleccionado.");
        }
        enfermedadRepository.delete(enfermedad);
        actualizarLista(pacienteId, "enfermedades");
    }

    public java.util.List<PacienteAlergia> alergias(Long pacienteId) {
        return alergiaRepository.findByPacienteId(pacienteId);
    }

    public java.util.List<PacienteMedicamento> medicamentos(Long pacienteId) {
        return medicamentoRepository.findByPacienteId(pacienteId);
    }

    @Transactional
    public void agregarMedicamento(Long pacienteId, MedicamentoForm formulario) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        paciente.agregarMedicamento(new PacienteMedicamento(
            formulario.getNombre().trim(), formulario.getDosis().trim(),
            formulario.getFrecuenciaHoras(), formulario.getHoraPrimeraDosis()));
        pacienteRepository.save(paciente);
        actualizarLista(pacienteId, "medicamentos");
    }

    @Transactional
    public void actualizarMedicamento(Long pacienteId, Long medicamentoId, MedicamentoForm formulario) {
        PacienteMedicamento medicamento = medicamentoRepository.findById(medicamentoId)
            .orElseThrow(() -> new ResourceNotFoundException("El medicamento solicitado no existe."));
        if (!medicamento.getPaciente().getId().equals(pacienteId)) {
            throw new IllegalArgumentException("El medicamento no pertenece al paciente seleccionado.");
        }
        medicamento.setNombre(formulario.getNombre().trim());
        medicamento.setDosis(formulario.getDosis().trim());
        medicamento.setFrecuenciaHoras(formulario.getFrecuenciaHoras());
        medicamento.setHoraPrimeraDosis(formulario.getHoraPrimeraDosis());
        medicamentoRepository.save(medicamento);
        actualizarLista(pacienteId, "medicamentos");
    }

    @Transactional
    public void eliminarMedicamento(Long pacienteId, Long medicamentoId) {
        PacienteMedicamento medicamento = medicamentoRepository.findById(medicamentoId)
            .orElseThrow(() -> new ResourceNotFoundException("El medicamento solicitado no existe."));
        if (!medicamento.getPaciente().getId().equals(pacienteId)) {
            throw new IllegalArgumentException("El medicamento no pertenece al paciente seleccionado.");
        }
        medicamentoRepository.delete(medicamento);
        actualizarLista(pacienteId, "medicamentos");
    }

    @Transactional
    public void agregarAlergia(Long pacienteId, AlergiaForm formulario) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        PacienteAlergia alergia = new PacienteAlergia(
            formulario.getNombre().trim(), formulario.getSeveridad());
        paciente.agregarAlergia(alergia);
        pacienteRepository.save(paciente);
        actualizarLista(pacienteId, "alergias");
    }

    @Transactional
    public void actualizarAlergia(Long pacienteId, Long alergiaId, AlergiaForm formulario) {
        PacienteAlergia alergia = alergiaRepository.findById(alergiaId)
            .orElseThrow(() -> new ResourceNotFoundException("La alergia solicitada no existe."));
        if (!alergia.getPaciente().getId().equals(pacienteId)) {
            throw new IllegalArgumentException("La alergia no pertenece al paciente seleccionado.");
        }
        alergia.setNombre(formulario.getNombre().trim());
        alergia.setSeveridad(formulario.getSeveridad());
        alergiaRepository.save(alergia);
        actualizarLista(pacienteId, "alergias");
    }

    @Transactional
    public void eliminarAlergia(Long pacienteId, Long alergiaId) {
        PacienteAlergia alergia = alergiaRepository.findById(alergiaId)
            .orElseThrow(() -> new ResourceNotFoundException("La alergia solicitada no existe."));
        if (!alergia.getPaciente().getId().equals(pacienteId)) {
            throw new IllegalArgumentException("La alergia no pertenece al paciente seleccionado.");
        }
        alergiaRepository.delete(alergia);
        actualizarLista(pacienteId, "alergias");
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

    public PacienteForm consultarParaEditar(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        PacienteForm formulario = new PacienteForm();
        formulario.setCodigo(paciente.getCodigo());
        formulario.setNombres(paciente.getNombres());
        formulario.setApellidos(paciente.getApellidos());
        formulario.setFechaNacimiento(paciente.getFechaNacimiento());
        formulario.setPesoKg(paciente.getPesoKg());
        formulario.setAlturaCm(paciente.getAlturaCm());
        formulario.setMedicoResponsableId(paciente.getMedicoResponsable() != null
            ? paciente.getMedicoResponsable().getId() : null);
        if (paciente.getCama() != null) {
            formulario.setPisoId(paciente.getCama().getPiso().getId());
            formulario.setCamaId(paciente.getCama().getId());
        }
        return formulario;
    }

    public java.util.List<Cama> camasDisponibles(Long pisoId) {
        if (pisoId == null) {
            return java.util.List.of();
        }
        return camaRepository.buscarDisponibles(pisoId, EstadoPaciente.INGRESADO);
    }

    public java.util.List<Cama> camasDisponiblesEdicion(Long pisoId, Long pacienteId) {
        java.util.ArrayList<Cama> camas = new java.util.ArrayList<>(camasDisponibles(pisoId));
        Paciente paciente = pacienteRepository.findById(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        if (paciente.getCama() != null && paciente.getCama().getPiso().getId().equals(pisoId)) {
            camas.removeIf(cama -> cama.getId().equals(paciente.getCama().getId()));
            camas.add(paciente.getCama());
            camas.sort(java.util.Comparator.comparing(Cama::getNumero));
        }
        return camas;
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

        var pacientesIngresados = pacienteRepository.findByCamaIdAndEstado(
            cama.getId(), EstadoPaciente.INGRESADO);
        if (!pacientesIngresados.isEmpty()) {
            throw new IllegalArgumentException("La cama seleccionada ya está ocupada por otro paciente ingresado.");
        }

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
        Paciente guardado = pacienteRepository.saveAndFlush(paciente);
        bitacora.registrar("CREAR", "pacientes", guardado.getId(), usuarioActual().getId());
        return guardado;
    }

    @Transactional
    public Paciente actualizar(Long id, PacienteForm formulario) {
        Paciente paciente = pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));

        Cama cama = camaRepository.findById(formulario.getCamaId())
            .orElseThrow(() -> new IllegalArgumentException("La cama seleccionada ya no existe."));
        if (!cama.getPiso().getId().equals(formulario.getPisoId())) {
            throw new IllegalArgumentException("La cama seleccionada no pertenece al piso indicado.");
        }

        Long camaActualId = paciente.getCama() == null ? null : paciente.getCama().getId();
        if (!cama.getId().equals(camaActualId)) {
            boolean ocupada = pacienteRepository.findByCamaIdAndEstado(
                cama.getId(), EstadoPaciente.INGRESADO).stream()
                .anyMatch(ocupante -> !ocupante.getId().equals(paciente.getId()));
            if (ocupada) {
                throw new IllegalArgumentException("La cama seleccionada ya está ocupada por otro paciente ingresado.");
            }
        }

        paciente.setNombres(formulario.getNombres().trim());
        paciente.setApellidos(formulario.getApellidos().trim());
        paciente.setFechaNacimiento(formulario.getFechaNacimiento());
        paciente.setPesoKg(formulario.getPesoKg());
        paciente.setAlturaCm(formulario.getAlturaCm());
        paciente.setCama(cama);
        Paciente guardado = pacienteRepository.saveAndFlush(paciente);
        bitacora.registrar("ACTUALIZAR", "pacientes", guardado.getId(), usuarioActual().getId());
        return guardado;
    }

    @Transactional
    public sv.edu.ues.alimentacion_hospitalaria.pacientes.web.PacientesController.PacienteAltaVista darDeAlta(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        if (paciente.getEstado() == EstadoPaciente.ALTA) {
            throw new IllegalArgumentException("El paciente ya se encuentra dado de alta.");
        }

        String ubicacion = paciente.getCama() == null
            ? "Sin ubicación"
            : paciente.getCama().getPiso().getCodigo() + "-" + paciente.getCama().getNumero();
        paciente.setEstado(EstadoPaciente.ALTA);
        pacienteRepository.saveAndFlush(paciente);
        bitacora.registrar("DAR_ALTA", "pacientes", paciente.getId(), usuarioActual().getId());
        return new sv.edu.ues.alimentacion_hospitalaria.pacientes.web.PacientesController.PacienteAltaVista(
            paciente.getCodigo(), ubicacion);
    }

    @Transactional
    public sv.edu.ues.alimentacion_hospitalaria.pacientes.web.PacientesController.PacienteAltaVista reingresar(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        if (paciente.getEstado() == EstadoPaciente.INGRESADO) {
            throw new IllegalArgumentException("El paciente ya se encuentra ingresado.");
        }
        if (paciente.getCama() == null) {
            throw new IllegalArgumentException(
                "La ubicación del paciente debe editarse antes de reingresar.");
        }

        Cama cama = paciente.getCama();
        boolean ocupada = pacienteRepository.findByCamaIdAndEstado(
            cama.getId(), EstadoPaciente.INGRESADO).stream()
            .anyMatch(ocupante -> !ocupante.getId().equals(paciente.getId()));
        if (ocupada) {
            throw new IllegalArgumentException(
                "La cama asignada ya está ocupada. Edite la ubicación del paciente antes de reingresar.");
        }
        paciente.setEstado(EstadoPaciente.INGRESADO);
        pacienteRepository.save(paciente);
        bitacora.registrar("REINGRESAR", "pacientes", paciente.getId(), usuarioActual().getId());
        String ubicacion = cama.getPiso().getCodigo() + "-" + cama.getNumero();
        return new sv.edu.ues.alimentacion_hospitalaria.pacientes.web.PacientesController.PacienteAltaVista(
            paciente.getCodigo(), ubicacion);
    }

    private void actualizarLista(Long pacienteId, String lista) {
        bitacora.registrar("se actualizo lista de " + lista, "pacientes", pacienteId, usuarioActual().getId());
    }

    private Usuario usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        return usuarioRepository.findByNombreUsuarioIgnoreCase(autenticacion.getName())
            .orElseThrow(() -> new AccessDeniedException("Acceso denegado."));
    }
}
