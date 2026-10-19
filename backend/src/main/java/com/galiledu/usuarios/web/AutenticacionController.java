package com.galiledu.usuarios.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.usuarios.aplicacion.Dtos.RespuestaLogin;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudLogin;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioSesion;
import com.galiledu.usuarios.aplicacion.ServicioAutenticacion;

import jakarta.validation.Valid;

/** HU-1. Contrato usado por frontend/src/lib/auth.tsx. */
@RestController
@RequestMapping("/auth")
public class AutenticacionController {
	private final ServicioAutenticacion autenticacion;

	public AutenticacionController(ServicioAutenticacion autenticacion) {
		this.autenticacion = autenticacion;
	}

	/** POST /api/auth/login { username, password } → { jwt, user }. 401 si falla, 423 si está bloqueada. */
	@PostMapping("/login")
	public RespuestaLogin login(@Valid @RequestBody SolicitudLogin solicitud) {
		return autenticacion.iniciarSesion(solicitud.username(), solicitud.password());
	}

	/** GET /api/auth/me → { data: user }. */
	@GetMapping("/me")
	public Map<String, UsuarioSesion> yo(Authentication autenticacion) {
		return Map.of("data", this.autenticacion.usuarioActual(ServicioJwt.usuarioActual(autenticacion)));
	}

	/** El token no se guarda en el servidor: cerrar sesión es borrarlo en el frontend. */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent().build();
	}
}
