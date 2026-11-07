package sv.edu.ues.alimentacion_hospitalaria.pacientes.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteDetalleVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.dto.PacienteVista;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteAlergiaRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteEnfermedadRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteMedicamentoRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AdministracionPacientesService {

    private final PacienteRepository pacienteRepository;
    private final PacienteEnfermedadRepository enfermedadRepository;
    private final PacienteAlergiaRepository alergiaRepository;
    private final PacienteMedicamentoRepository medicamentoRepository;

    public AdministracionPacientesService(PacienteRepository pacienteRepository,
                                          PacienteEnfermedadRepository enfermedadRepository,
                                          PacienteAlergiaRepository alergiaRepository,
                                          PacienteMedicamentoRepository medicamentoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.enfermedadRepository = enfermedadRepository;
        this.alergiaRepository = alergiaRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    public Page<PacienteVista> listar(int pagina, String buscar, EstadoPaciente estado) {
        String filtro = buscar == null ? "" : buscar.trim();
        Pageable pageable = PageRequest.of(Math.max(0, pagina), 20,
            Sort.by(Sort.Order.asc("codigo"), Sort.Order.asc("id")));
        return pacienteRepository.buscar(filtro, estado, pageable).map(PacienteVista::desde);
    }

    public PacienteDetalleVista consultar(Long id) {
        var paciente = pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("El paciente solicitado no existe."));
        paciente.getEnfermedades().size();
        paciente.getAlergias().size();
        paciente.getMedicamentos().size();
        return PacienteDetalleVista.desde(paciente);
    }
}
