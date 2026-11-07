package sv.edu.ues.alimentacion_hospitalaria.insumos.dto;

public enum PeriodoProyeccion {
    SEMANAL(7, "Siguientes 7 días"),
    MENSUAL(30, "Siguientes 30 días");

    private final int dias;
    private final String etiqueta;

    PeriodoProyeccion(int dias, String etiqueta) {
        this.dias = dias;
        this.etiqueta = etiqueta;
    }

    public int getDias() {
        return dias;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
