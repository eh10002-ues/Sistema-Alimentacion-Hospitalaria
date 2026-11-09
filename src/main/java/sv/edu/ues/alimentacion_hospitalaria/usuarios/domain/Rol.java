package sv.edu.ues.alimentacion_hospitalaria.usuarios.domain;

public enum Rol {

	ROLE_ADMIN("Administrador"),
	ROLE_MEDICO("Médico"),
	ROLE_NUTRICIONISTA("Nutricionista"),
	ROLE_ENCARGADO_PISO("Encargado de piso"),
	ROLE_COCINA("Personal de cocina");

	private final String etiqueta;

	Rol(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}
}
