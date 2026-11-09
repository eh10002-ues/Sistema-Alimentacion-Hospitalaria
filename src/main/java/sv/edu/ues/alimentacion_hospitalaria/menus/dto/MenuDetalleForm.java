package sv.edu.ues.alimentacion_hospitalaria.menus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.MenuDetalle;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TiempoComida;

@Getter
@Setter
public class MenuDetalleForm {

    @NotNull(message = "Seleccione el tiempo de comida.")
    private TiempoComida tiempoComida;

    @NotNull(message = "Ingrese la hora de servicio.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaServicio;

    @NotBlank(message = "Ingrese el platillo.")
    @Size(max = 150, message = "El platillo admite hasta 150 caracteres.")
    private String platillo;

    public static MenuDetalleForm desde(MenuDetalle detalle) {
        MenuDetalleForm formulario = new MenuDetalleForm();
        formulario.setTiempoComida(detalle.getTiempoComida());
        formulario.setHoraServicio(detalle.getHoraServicio());
        formulario.setPlatillo(detalle.getPlatillo());
        return formulario;
    }
}
