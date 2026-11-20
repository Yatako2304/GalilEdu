package com.galiledu.usuarios.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.personas.aplicacion.DatosPersona;
import com.galiledu.usuarios.aplicacion.Dtos.FamiliaApoderado;
import com.galiledu.usuarios.aplicacion.Dtos.Mensaje;
import com.galiledu.usuarios.aplicacion.Dtos.MetodosAcceso;
import com.galiledu.usuarios.aplicacion.Dtos.RespuestaLogin;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudCambioContrasena;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudGoogle;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudLogin;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudRecuperacion;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudRestablecer;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudVerificarCodigo;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudVerificarContrasena;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioSesion;
import com.galiledu.usuarios.aplicacion.ServicioAccesoGoogle;
import com.galiledu.usuarios.aplicacion.ServicioAutenticacion;
import com.galiledu.usuarios.aplicacion.ServicioCambioContrasena;
import com.galiledu.usuarios.aplicacion.ServicioMiFamilia;
import com.galiledu.usuarios.aplicacion.ServicioRecuperacionContrasena;
import com.galiledu.usuarios.aplicacion.ServicioUsuarios;

/** HU-1, HU-3, HU-4 y HU-5. Contrato usado por frontend/src/lib/auth.tsx. */
@RestController
@RequestMapping("/auth")
public class AutenticacionController {
	private final ServicioAutenticacion autenticacion;
	private final ServicioRecuperacionContrasena recuperacion;
	private final ServicioCambioContrasena cambio;
	private final ServicioAccesoGoogle google;
	private final ServicioUsuarios usuarios;
	private final ServicioMiFamilia familia;

	public AutenticacionController(ServicioAutenticacion autenticacion,
			ServicioRecuperacionContrasena recuperacion, ServicioCambioContrasena cambio,
			ServicioAccesoGoogle google, ServicioUsuarios usuarios, ServicioMiFamilia familia) {
		this.autenticacion = autenticacion;
		this.recuperacion = recuperacion;
		this.cambio = cambio;
		this.google = google;
		this.usuarios = usuarios;
		this.familia = familia;
	}

	/** POST /api/auth/login { username, password } → { jwt, user }. 401 si falla, 423 si está bloqueada. */
	@PostMapping("/login")
	public RespuestaLogin login(@Valid @RequestBody SolicitudLogin solicitud) {
		return autenticacion.iniciarSesion(solicitud.username(), solicitud.password());
	}

	/** HU-2 · GET /api/auth/metodos (público) → { google, googleIdCliente }: si se muestra el botón de Google. */
	@GetMapping("/metodos")
	public MetodosAcceso metodos() {
		return google.metodos();
	}

	/**
	 * HU-3 · POST /api/auth/google { codigo } → { jwt, user }. 403 si Google no está habilitado, 401 si Google no
	 * verifica la cuenta, 404 si ningún usuario tiene registrado ese correo, 423 si la cuenta está bloqueada.
	 */
	@PostMapping("/google")
	public RespuestaLogin loginGoogle(@Valid @RequestBody SolicitudGoogle solicitud) {
		return google.iniciarSesion(solicitud.codigo());
	}

	/** GET /api/auth/me → { data: user }. */
	@GetMapping("/me")
	public Map<String, UsuarioSesion> yo(Authentication autenticacion) {
		return Map.of("data", this.autenticacion.usuarioActual(ServicioJwt.usuarioActual(autenticacion)));
	}

	/**
	 * Cualquier usuario autenticado, sin importar el rol · GET /api/auth/mis-datos → sus propios datos
	 * personales. Se abre desde la tarjeta de usuario del menú lateral (antes abría «Cambiar contraseña»
	 * directamente); no está en Figma, que no tiene una pantalla «Mis datos».
	 */
	@GetMapping("/mis-datos")
	public DatosPersona misDatos(Authentication autenticacion) {
		return usuarios.misDatos(ServicioJwt.usuarioActual(autenticacion));
	}

	/**
	 * PUT /api/auth/mis-datos { ...DatosPersona } → UsuarioSesion. Mismas validaciones y control de duplicados que
	 * «Editar usuario» (HU-9), pero sobre la propia cuenta y sin tocar roles ni el estado de la cuenta.
	 */
	@PutMapping("/mis-datos")
	public UsuarioSesion actualizarMisDatos(@Valid @RequestBody DatosPersona solicitud, Authentication autenticacion) {
		return usuarios.actualizarMisDatos(ServicioJwt.usuarioActual(autenticacion), solicitud);
	}

	/**
	 * Solo Apoderado · GET /api/auth/mi-familia → familias de sus estudiantes a cargo (padre, madre, apoderado y
	 * contactos de emergencia), de solo vista. Figma › Apoderado › «Datos de la Familia».
	 */
	@GetMapping("/mi-familia")
	@PreAuthorize("hasRole('APODERADO')")
	public List<FamiliaApoderado> miFamilia(Authentication autenticacion) {
		return familia.deUsuario(ServicioJwt.usuarioActual(autenticacion));
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
