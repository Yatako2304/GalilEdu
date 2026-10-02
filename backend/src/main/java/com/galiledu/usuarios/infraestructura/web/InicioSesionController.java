package com.galiledu.usuarios.infraestructura.web;

import com.galiledu.usuarios.infraestructura.web.CsrfController.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST para HU-1; reutiliza el mismo proveedor de HTTP Basic. */
@RestController
@RequestMapping("/api/auth")
public class InicioSesionController {
	private final AuthenticationProvider autenticacion;
	private final SecurityContextRepository repositorioContexto;

	public InicioSesionController(AuthenticationProvider autenticacion,
		SecurityContextRepository repositorioContexto) {
		this.autenticacion = autenticacion;
		this.repositorioContexto = repositorioContexto;
	}

	@PostMapping("/login")
	public ResponseEntity<CuentaActual> iniciar(@RequestBody SolicitudAcceso solicitud,
		HttpServletRequest request, HttpServletResponse response) {
		if (solicitud == null || solicitud.nombreUsuario() == null || solicitud.nombreUsuario().isBlank()
			|| solicitud.contrasena() == null || solicitud.contrasena().isBlank()) {
			throw new BadCredentialsException("Credenciales incorrectas");
		}
		Authentication cuenta = autenticacion.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated(
				solicitud.nombreUsuario(), solicitud.contrasena()));
		if (request.getSession(false) != null) request.changeSessionId();
		var contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(cuenta);
		SecurityContextHolder.setContext(contexto);
		repositorioContexto.saveContext(contexto, request, response);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(CuentaActual.desde(cuenta));
	}

	public record SolicitudAcceso(String nombreUsuario, String contrasena) {
		@Override
		public String toString() {
			return "SolicitudAcceso[credenciales ocultas]";
		}
	}
}
