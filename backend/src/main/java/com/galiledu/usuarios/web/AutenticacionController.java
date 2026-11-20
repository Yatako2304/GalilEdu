package com.galiledu.usuarios.web;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.usuarios.aplicacion.Dtos.Mensaje;
import com.galiledu.usuarios.aplicacion.Dtos.RespuestaLogin;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudCambioContrasena;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudLogin;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudRecuperacion;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudRestablecer;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudVerificarCodigo;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudVerificarContrasena;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioSesion;
import com.galiledu.usuarios.aplicacion.ServicioAutenticacion;
import com.galiledu.usuarios.aplicacion.ServicioCambioContrasena;
import com.galiledu.usuarios.aplicacion.ServicioRecuperacionContrasena;

/** HU-1, HU-4 y HU-5. Contrato usado por frontend/src/lib/auth.tsx. */
@RestController
@RequestMapping("/auth")
public class AutenticacionController {
	private final ServicioAutenticacion autenticacion;
	private final ServicioRecuperacionContrasena recuperacion;
	private final ServicioCambioContrasena cambio;

	public AutenticacionController(ServicioAutenticacion autenticacion,
			ServicioRecuperacionContrasena recuperacion, ServicioCambioContrasena cambio) {
		this.autenticacion = autenticacion;
		this.recuperacion = recuperacion;
		this.cambio = cambio;
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

	/**
	 * HU-4 · POST /api/auth/recuperar-contrasena { email } → envía un código de 6 dígitos al correo (también es
	 * «Reenviar código»). 404 si el correo no es de ninguna cuenta, 429 si no pasaron 60 s desde el anterior.
	 */
	@PostMapping("/recuperar-contrasena")
	public Mensaje solicitarCodigo(@Valid @RequestBody SolicitudRecuperacion solicitud) {
		recuperacion.solicitar(solicitud.email());
		return new Mensaje("Te enviamos un código de verificación a tu correo.");
	}

	/** HU-4 · POST /api/auth/recuperar-contrasena/verificar { email, codigo }. 400 si es incorrecto o venció. */
	@PostMapping("/recuperar-contrasena/verificar")
	public Mensaje verificarCodigo(@Valid @RequestBody SolicitudVerificarCodigo solicitud) {
		recuperacion.verificar(solicitud.email(), solicitud.codigo());
		return new Mensaje("Código verificado.");
	}

	/** HU-4 · POST /api/auth/recuperar-contrasena/restablecer { email, codigo, contrasena }. */
	@PostMapping("/recuperar-contrasena/restablecer")
	public Mensaje restablecer(@Valid @RequestBody SolicitudRestablecer solicitud) {
		recuperacion.restablecer(solicitud.email(), solicitud.codigo(), solicitud.contrasena());
		return new Mensaje("Su contraseña ha sido actualizada correctamente.");
	}

	/**
	 * HU-5 · POST /api/auth/cambiar-contrasena/verificar { contrasenaActual } (con sesión). 400 «Contraseña
	 * incorrecta.»; al quinto intento fallido la cuenta se bloquea 15 minutos (423), como en el inicio de sesión.
	 */
	@PostMapping("/cambiar-contrasena/verificar")
	public Mensaje verificarContrasenaActual(@Valid @RequestBody SolicitudVerificarContrasena solicitud,
			Authentication autenticacion) {
		cambio.verificarActual(ServicioJwt.usuarioActual(autenticacion), solicitud.contrasenaActual());
		return new Mensaje("Contraseña verificada.");
	}

	/**
	 * HU-5 · POST /api/auth/cambiar-contrasena { contrasenaActual, contrasenaNueva } (con sesión): guarda la nueva y
	 * cierra las sesiones en todos los dispositivos (el token usado aquí también deja de valer).
	 */
	@PostMapping("/cambiar-contrasena")
	public Mensaje cambiarContrasena(@Valid @RequestBody SolicitudCambioContrasena solicitud,
			Authentication autenticacion) {
		cambio.cambiar(ServicioJwt.usuarioActual(autenticacion), solicitud.contrasenaActual(),
				solicitud.contrasenaNueva());
		return new Mensaje("Su contraseña ha sido actualizada correctamente.");
	}
}
