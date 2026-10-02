package com.galiledu.usuarios.infraestructura.web;

import com.galiledu.usuarios.aplicacion.CambiarContrasena;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CredencialesController {
	private final CambiarContrasena cambiar;

	public CredencialesController(CambiarContrasena cambiar) {
		this.cambiar = cambiar;
	}

	@PutMapping("/password")
	public ResponseEntity<Void> cambiarContrasena(@RequestBody CambioContrasenaRequest solicitud,
		Authentication autenticacion, HttpServletRequest request) {
		if (solicitud == null) throw new IllegalArgumentException("Los datos de contraseña son obligatorios");
		cambiar.ejecutar(autenticacion.getName(), solicitud.contrasenaActual(), solicitud.nuevaContrasena());
		HttpSession sesion = request.getSession(false);
		if (sesion != null) sesion.invalidate();
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorRespuesta> solicitudInvalida(IllegalArgumentException error) {
		return ResponseEntity.badRequest().body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorRespuesta> conflicto(IllegalStateException error) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorRespuesta(error.getMessage()));
	}

	public record CambioContrasenaRequest(String contrasenaActual, String nuevaContrasena) {
		@Override
		public String toString() {
			return "CambioContrasenaRequest[credenciales ocultas]";
		}
	}
	public record ErrorRespuesta(String mensaje) {}
}
