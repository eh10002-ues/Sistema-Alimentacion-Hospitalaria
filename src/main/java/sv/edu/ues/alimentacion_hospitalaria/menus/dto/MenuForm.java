package sv.edu.ues.alimentacion_hospitalaria.menus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.Menu;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoDieta;
import sv.edu.ues.alimentacion_hospitalaria.menus.domain.TipoMenu;

@Getter
@Setter
public class MenuForm {

    @NotBlank(message = "Ingrese el nombre del menú.")
    @Size(max = 150, message = "El nombre admite hasta 150 caracteres.")
    private String nombre;

    @NotNull(message = "Seleccione el tipo de menú.")
    private TipoMenu tipo = TipoMenu.GENERAL;

    @NotNull(message = "Seleccione el tipo de dieta.")
    private TipoDieta tipoDieta;

    public static MenuForm desde(Menu menu) {
        MenuForm formulario = new MenuForm();
        formulario.setNombre(menu.getNombre());
        formulario.setTipo(menu.getTipo());
        formulario.setTipoDieta(menu.getTipoDieta());
        return formulario;
    }
}
