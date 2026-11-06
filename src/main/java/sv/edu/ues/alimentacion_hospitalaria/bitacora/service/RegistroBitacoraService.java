package sv.edu.ues.alimentacion_hospitalaria.bitacora.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.domain.Bitacora;
import sv.edu.ues.alimentacion_hospitalaria.bitacora.persistence.BitacoraRepository;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence.UsuarioRepository;

@Service
public class RegistroBitacoraService {

    private final BitacoraRepository bitacoraRepository;
    private final UsuarioRepository usuarioRepository;

    public RegistroBitacoraService(BitacoraRepository bitacoraRepository,
                                   UsuarioRepository usuarioRepository) {
        this.bitacoraRepository = bitacoraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra una acción en la bitácora.
     *
     * @param accion        qué se hizo (ej. "CREAR", "ACTUALIZAR", "APROBAR")
     * @param tablaAfectada tabla sobre la que se actuó (ej. "pacientes")
     * @param idAfectado    id del registro afectado (puede ser null)
     * @param usuarioId     id del usuario que ejecutó la acción
     */
    @Transactional
    public void registrar(String accion, String tablaAfectada, Long idAfectado, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el usuario " + usuarioId + " para registrar en la bitácora."));

        bitacoraRepository.save(new Bitacora(accion, tablaAfectada, idAfectado, usuario));
    }

}
