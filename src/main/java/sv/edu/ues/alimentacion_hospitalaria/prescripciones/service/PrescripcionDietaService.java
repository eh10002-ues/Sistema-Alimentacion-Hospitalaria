package sv.edu.ues.alimentacion_hospitalaria.prescripciones.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sv.edu.ues.alimentacion_hospitalaria.bitacora.service.RegistroBitacoraService;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.RegimenDietetico;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto.EvaluacionPaciente;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto.OrdenCocina;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto.PacienteResumen;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto.PrescripcionVista;
import sv.edu.ues.alimentacion_hospitalaria.prescripciones.persistence.PrescripcionDietaRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/** SAH-2: evaluación del paciente y autorización de su régimen dietético. */
@Service
@PreAuthorize("hasAnyRole('MEDICO', 'NUTRICIONISTA')")
@Transactional(readOnly = true)
public class PrescripcionDietaService {

    static final String ACCION_BITACORA = "APROBAR";
    static final String TABLA_BITACORA = "prescripciones_dieta";

    private final PrescripcionDietaRepository prescripciones;
    private final PacienteRepository pacientes;
    private final UsuarioRepository usuarios;
    private final RegistroBitacoraService bitacora;
    private final ApplicationEventPublisher eventos;

    public PrescripcionDietaService(PrescripcionDietaRepository prescripciones, PacienteRepository pacientes,
                                    UsuarioRepository usuarios, RegistroBitacoraService bitacora,
                                    ApplicationEventPublisher eventos) {
        this.prescripciones = prescripciones;
        this.pacientes = pacientes;
        this.usuarios = usuarios;
        this.bitacora = bitacora;
        this.eventos = eventos;
    }

    public List<PacienteResumen> listarPacientesIngresados() {
        return pacientes.findByEstado(EstadoPaciente.INGRESADO).stream()
            .sorted(Comparator.comparing(Paciente::getApellidos).thenComparing(Paciente::getNombres))
            .map(PacienteResumen::desde).toList();
    }

    public EvaluacionPaciente evaluar(Long pacienteId) {
        Paciente paciente = buscarPaciente(pacienteId);
        var historial = prescripciones.findByPacienteIdOrderByFechaAutorizacionDescIdDesc(pacienteId).stream()
            .map(PrescripcionVista::desde).toList();
        return EvaluacionPaciente.desde(paciente, historial);
    }

    /**
     * Crea la prescripción directamente en estado APROBADO, la registra en la bitácora
     * y publica el evento que se envía a cocina cuando la transacción se confirma.
     */
    @Transactional
    public PrescripcionVista autorizar(Long pacienteId, RegimenDietetico regimen) {
        Usuario autorizador = usuarioActual();
        Paciente paciente = buscarPaciente(pacienteId);
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        PrescripcionDieta prescripcion = prescripciones.save(
            PrescripcionDieta.autorizar(paciente, regimen, autorizador, ahora));
        bitacora.registrar(ACCION_BITACORA, TABLA_BITACORA, prescripcion.getId(), autorizador.getId());
        eventos.publishEvent(new PrescripcionAutorizada(OrdenCocina.desde(prescripcion)));
        return PrescripcionVista.desde(prescripcion);
    }

    private Paciente buscarPaciente(Long id) {
        return pacientes.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
    }

    private Usuario usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            throw new AccessDeniedException("Acceso denegado.");
        }
        return usuarios.findByNombreUsuarioIgnoreCase(autenticacion.getName())
            .orElseThrow(() -> new AccessDeniedException("Acceso denegado."));
    }
}
