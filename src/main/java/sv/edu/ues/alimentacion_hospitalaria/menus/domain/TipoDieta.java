package sv.edu.ues.alimentacion_hospitalaria.menus.domain;

public enum TipoDieta {
    NORMAL("Normal"),
    BLANDA("Blanda"),
    LIQUIDA("Líquida"),
    HIPOSODICA("Hiposódica"),
    HIPOCALORICA("Hipocalórica"),
    PAPILLA("Papilla");

    private final String etiqueta;

    TipoDieta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
