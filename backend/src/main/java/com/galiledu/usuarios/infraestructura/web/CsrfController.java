package com.galiledu.usuarios.infraestructura.web;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CsrfController {
	@GetMapping("/csrf")
	public CsrfRespuesta csrf(CsrfToken token) {
		return new CsrfRespuesta(token.getHeaderName(), token.getToken());
	}

	@GetMapping("/me")
	public CuentaActual me(Authentication autenticacion) {
		return CuentaActual.desde(autenticacion);
	}

	public record CsrfRespuesta(String headerName, String token) {}
	public record CuentaActual(String nombreUsuario, List<String> roles) {
		public static CuentaActual desde(Authentication autenticacion) {
			List<String> roles = autenticacion.getAuthorities().stream()
				.map(autoridad -> autoridad.getAuthority().replaceFirst("^ROLE_", ""))
				.sorted().toList();
			return new CuentaActual(autenticacion.getName(), roles);
		}
	}
}
