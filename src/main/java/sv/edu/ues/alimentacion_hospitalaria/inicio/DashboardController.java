package sv.edu.ues.alimentacion_hospitalaria.inicio;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class DashboardController {
	private final DashboardService dashboard;

	public DashboardController(DashboardService dashboard) {
		this.dashboard = dashboard;
	}

	@GetMapping("/dashboard")
	String inicio(Authentication authentication, Model model) {
		model.addAttribute("panel", dashboard.construir(authentication));
		return "index";
	}

	@GetMapping("/acceso-denegado")
	@ResponseStatus(HttpStatus.FORBIDDEN)
	String accesoDenegado() {
		return "error/403";
	}
}
