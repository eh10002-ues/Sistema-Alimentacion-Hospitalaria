package sv.edu.ues.alimentacion_hospitalaria.usuarios.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Usuario;
import sv.edu.ues.alimentacion_hospitalaria.usuarios.domain.Rol;

// Solo se exponen operaciones necesarias para conservar los registros de usuarios.
public interface UsuarioRepository extends Repository<Usuario, Long> {

	Usuario save(Usuario usuario);

	Optional<Usuario> findById(Long id);

	Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

	boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

	boolean existsByRol(Rol rol);

	Page<Usuario> findAll(Pageable pageable);
}
