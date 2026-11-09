package sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain;

/**
 * SAH-2 crea la prescripción directamente aprobada al pulsar «Autorizar».
 * No existe un flujo previo de pendientes.
 */
public enum EstadoPrescripcion {
    APROBADO("Aprobado");

    private final String etiqueta;

    EstadoPrescripcion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
