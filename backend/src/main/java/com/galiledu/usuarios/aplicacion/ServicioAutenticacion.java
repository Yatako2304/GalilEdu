package com.galiledu.usuarios.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.usuarios.aplicacion.Dtos.RespuestaLogin;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioSesion;
import com.galiledu.usuarios.dominio.Usuario;
import com.galiledu.usuarios.infraestructura.UsuarioRepositorio;

/** HU-1: inicio de sesión con usuario y contraseña (RF-1, RF-2, RF-3). */
@Service
public class ServicioAutenticacion {
	static final String CREDENCIALES_INVALIDAS = "El usuario o contraseña son incorrectos.";
	static final String CUENTA_DESACTIVADA = "Tu cuenta está desactivada. Comunícate con el administrador del colegio.";

	private final UsuarioRepositorio usuarios;
	private final PasswordEncoder contrasenas;
	private final ServicioJwt jwt;
	private final Clock reloj;
	private final String hashFicticio;

	public ServicioAutenticacion(UsuarioRepositorio usuarios, PasswordEncoder contrasenas, ServicioJwt jwt) {
		this.usuarios = usuarios;
		this.contrasenas = contrasenas;
		this.jwt = jwt;
		this.reloj = Clock.systemUTC();
		// Se compara contra un hash falso cuando el usuario no existe, para no revelar qué cuentas existen.
		this.hashFicticio = contrasenas.encode("cuenta-inexistente");
	}

	/** El bloqueo y los intentos fallidos se guardan aunque el login falle (por eso noRollbackFor). */
	@Transactional(noRollbackFor = ExcepcionNegocio.class)
	public RespuestaLogin iniciarSesion(String nombreUsuario, String contrasena) {
		Usuario usuario = usuarios.findByNombreUsuarioIgnoreCase(nombreUsuario.strip()).orElse(null);
		if (usuario == null) {
			contrasenas.matches(contrasena, hashFicticio);
			throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
		}
		Instant ahora = reloj.instant();
		if (usuario.bloqueadoEn(ahora)) {
			throw bloqueada(Duration.between(ahora, usuario.getBloqueadoHasta()));
		}
		if (!contrasenas.matches(contrasena, usuario.getContrasenaHash())) {
			if (usuario.registrarFallo(ahora)) {
				throw bloqueada(Usuario.DURACION_BLOQUEO);
			}
			throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
		}
		if (!usuario.puedeIniciarSesion()) {
			throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, CUENTA_DESACTIVADA);
		}
		return entrar(usuario, ahora);
	}

	/**
	 * HU-3: inicio de sesión de una cuenta ya identificada por otro medio (Google). Respeta el bloqueo por intentos
	 * fallidos y la cuenta desactivada, igual que el inicio con contraseña. Debe llamarse dentro de una transacción.
	 */
	RespuestaLogin iniciarSesionVerificada(Usuario usuario) {
		Instant ahora = reloj.instant();
		if (usuario.bloqueadoEn(ahora)) {
			throw bloqueada(Duration.between(ahora, usuario.getBloqueadoHasta()));
		}
		if (!usuario.puedeIniciarSesion()) {
			throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, CUENTA_DESACTIVADA);
		}
		return entrar(usuario, ahora);
	}

	private RespuestaLogin entrar(Usuario usuario, Instant ahora) {
		usuario.registrarAcceso(ahora);
		var roles = usuario.getRoles().stream().map(Enum::name).toList();
		String token = jwt.emitir(usuario.getId(), usuario.getNombreUsuario(), roles);
		return new RespuestaLogin(token, UsuarioSesion.de(usuario));
	}

	@Transactional(readOnly = true)
	public UsuarioSesion usuarioActual(UUID id) {
		return usuarios.findById(id)
				.filter(Usuario::puedeIniciarSesion)
				.map(UsuarioSesion::de)
				.orElseThrow(() -> new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida"));
	}

	private static ExcepcionNegocio bloqueada(Duration restante) {
		long minutos = Math.max(1, (restante.toSeconds() + 59) / 60);
		return new ExcepcionNegocio(HttpStatus.LOCKED,
				"Cuenta bloqueada por varios intentos fallidos. Inténtalo de nuevo en " + minutos
						+ (minutos == 1 ? " minuto." : " minutos."));
	}
}
