package sv.edu.ues.alimentacion_hospitalaria.pacientes.service;

import org.springframework.stereotype.Service;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteAlergiaRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteEnfermedadRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteMedicamentoRepository;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;

@Service
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

    // TODO: registro de pacientes, datos clínicos y asignación de cama.
}
