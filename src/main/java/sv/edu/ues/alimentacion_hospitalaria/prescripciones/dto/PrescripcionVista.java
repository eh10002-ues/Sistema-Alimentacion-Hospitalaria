package sv.edu.ues.alimentacion_hospitalaria.prescripciones.dto;

import java.time.Instant;

import sv.edu.ues.alimentacion_hospitalaria.prescripciones.domain.PrescripcionDieta;

public record PrescripcionVista(Long id, String regimen, String estado, String autorizadoPor,
                                String rolAutorizador, Instant fechaAutorizacion) {

    public static PrescripcionVista desde(PrescripcionDieta prescripcion) {
        var autorizador = prescripcion.getAutorizadoPor();
        return new PrescripcionVista(prescripcion.getId(), prescripcion.getRegimen().getEtiqueta(),
            prescripcion.getEstado().getEtiqueta(), autorizador.getNombreCompleto(),
            autorizador.getRol().getEtiqueta(), prescripcion.getFechaAutorizacion());
    }
}
