package sv.edu.ues.alimentacion_hospitalaria.usuarios.domain;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import sv.edu.ues.alimentacion_hospitalaria.shared.domain.AuditableEntity;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(
	name = "uk_usuarios_nombre_usuario", columnNames = "nombre_usuario"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 100)
	@Pattern(regexp = "[^\\s\\p{Z}]+", message = "El nombre de usuario no puede contener espacios.")
	@Column(name = "nombre_usuario", nullable = false, length = 100)
	private String nombreUsuario;

	// Recibe exclusivamente el resultado del PasswordEncoder, nunca la contraseña original.
	@NotBlank
	@Size(max = 255)
	@Column(name = "contrasena_hash", nullable = false, length = 255)
	@Setter
	private String contrasenaHash;

	@NotBlank
	@Size(max = 150)
	@Column(name = "nombre_completo", nullable = false, length = 150)
	@Setter
	private String nombreCompleto;

	@NotNull
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 40)
	@Setter
	private Rol rol;

	@Column(nullable = false)
	@Setter
	private boolean activo;

	public Usuario(String nombreUsuario, String contrasenaHash, String nombreCompleto, Rol rol, boolean activo) {
		setNombreUsuario(nombreUsuario);
		this.contrasenaHash = contrasenaHash;
		this.nombreCompleto = nombreCompleto;
		this.rol = rol;
		this.activo = activo;
	}

	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario == null ? null : nombreUsuario.toLowerCase(Locale.ROOT);
	}

	@PrePersist
	@PreUpdate
	private void normalizarNombreUsuario() {
		setNombreUsuario(nombreUsuario);
	}
}
