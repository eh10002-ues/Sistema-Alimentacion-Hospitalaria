package sv.edu.ues.alimentacion_hospitalaria.prescripciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.EstadoPaciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.Paciente;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.PacienteEnfermedad;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.domain.SeveridadAlergia;
import sv.edu.ues.alimentacion_hospitalaria.pacientes.persistence.PacienteRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Cama;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.domain.Piso;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.CamaRepository;
import sv.edu.ues.alimentacion_hospitalaria.ubicaciones.persistence.PisoRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

/** Datos sintéticos mínimos para las pruebas de SAH-2. */
public record DatosClinicosPrueba(UsuarioRepository usuarios, PisoRepository pisos, CamaRepository camas,
                                  PacienteRepository pacientes) {

    public Usuario usuario(String nombre, Rol rol) {
        return usuarios.save(new Usuario(nombre, "{noop}no-se-usa", "Usuario " + nombre, rol, true));
    }

    public Paciente pacienteIngresado() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Piso piso = pisos.save(new Piso("P" + sufijo.substring(0, 4), "Piso de prueba"));
        Cama cama = camas.save(new Cama(piso, "C" + sufijo.substring(4)));
        Paciente paciente = new Paciente("PAC-" + sufijo, "Ana", "Pérez", LocalDate.of(1980, 5, 10), new BigDecimal("62.50"));
        // El constructor del modelo no recibe la altura, que es obligatoria.
        paciente.setAlturaCm(new BigDecimal("160.00"));
        paciente.setCama(cama);
        paciente.agregarEnfermedad(new PacienteEnfermedad("Hipertensión arterial"));
        paciente.agregarAlergia(new PacienteAlergia("Mariscos", SeveridadAlergia.SEVERA));
        return pacientes.save(paciente);
    }

    public Paciente pacienteDeAlta() {
        Paciente paciente = new Paciente("ALTA-" + UUID.randomUUID().toString().substring(0, 8), "Luis", "Gómez",
            LocalDate.of(1975, 1, 20), new BigDecimal("80.00"));
        paciente.setAlturaCm(new BigDecimal("175.00"));
        paciente.setEstado(EstadoPaciente.ALTA);
        return pacientes.save(paciente);
    }
}
