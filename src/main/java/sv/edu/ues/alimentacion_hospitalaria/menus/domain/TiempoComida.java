package sv.edu.ues.alimentacion_hospitalaria.menus.domain;

//las colaciones son refrigerios o snacks, tentenpies que pueden comer los pacientes
public enum TiempoComida {
    DESAYUNO("Desayuno"),
    COLACION_MATUTINA("Colación matutina"),
    ALMUERZO("Almuerzo"),
    COLACION_VESPERTINA("Colación vespertina"),
    CENA("Cena");

    private final String etiqueta;

    TiempoComida(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
