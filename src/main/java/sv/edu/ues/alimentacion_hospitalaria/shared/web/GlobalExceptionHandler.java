package sv.edu.ues.alimentacion_hospitalaria.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import sv.edu.ues.alimentacion_hospitalaria.shared.exception.ResourceNotFoundException;

// No capturar Exception aquí: interferiría con el manejo de AccessDeniedException de Spring Security
@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleNotFound(ResourceNotFoundException ex, Model model) {
		model.addAttribute("message", ex.getMessage());
		return "error/404";
	}
}
