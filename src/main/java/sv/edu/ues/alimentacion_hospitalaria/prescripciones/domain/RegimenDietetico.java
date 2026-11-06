package sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain;

import java.util.Arrays;

import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;

/**
 * Regímenes que se pueden prescribir en SAH-2. Se guardan como {@link TipoDieta}
 * para compartir los valores del módulo de menús; «Corriente» equivale a NORMAL.
 */
public enum RegimenDietetico {
    CORRIENTE(TipoDieta.NORMAL, "Corriente"),
    BLANDA(TipoDieta.BLANDA, "Blanda"),
    HIPOSODICA(TipoDieta.HIPOSODICA, "Hiposódica");

    private final TipoDieta tipoDieta;
    private final String etiqueta;

    RegimenDietetico(TipoDieta tipoDieta, String etiqueta) {
        this.tipoDieta = tipoDieta;
        this.etiqueta = etiqueta;
    }

    public TipoDieta getTipoDieta() {
        return tipoDieta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static RegimenDietetico desde(TipoDieta tipoDieta) {
        return Arrays.stream(values()).filter(regimen -> regimen.tipoDieta == tipoDieta).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("El tipo de dieta no es un régimen prescribible."));
    }
}
