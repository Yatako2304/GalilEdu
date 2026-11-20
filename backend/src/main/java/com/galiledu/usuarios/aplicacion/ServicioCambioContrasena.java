package com.galiledu.usuarios.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.usuarios.dominio.PoliticaContrasena;
import com.galiledu.usuarios.dominio.Usuario;
import com.galiledu.usuarios.infraestructura.UsuarioRepositorio;

/**
 * HU-5 (Figma › Autenticación › Cambiar contraseña): el usuario con sesión escribe su contraseña actual y luego elige
 * una nueva. Al cambiarla se cierran sus sesiones en todos los dispositivos ({@link Usuario#reemplazarContrasena}).
 * Una contraseña actual incorrecta cuenta como intento fallido, igual que en el inicio de sesión (RF-2), para que no
 * se pueda adivinar desde una sesión abierta.
 */
@Service
public class ServicioCambioContrasena {
	static final String CONTRASENA_INCORRECTA = "Contraseña incorrecta.";

	private final UsuarioRepositorio usuarios;
	private final PasswordEncoder hashes;
	private final Clock reloj;

	public ServicioCambioContrasena(UsuarioRepositorio usuarios, PasswordEncoder hashes, Clock reloj) {
		this.usuarios = usuarios;
		this.hashes = hashes;
		this.reloj = reloj;
	}

	/** Paso 1 «Continuar». El intento fallido se guarda aunque la respuesta sea un error (noRollbackFor). */
	@Transactional(noRollbackFor = ExcepcionNegocio.class)
	public void verificarActual(UUID usuarioId, String actual) {
		comprobarActual(usuario(usuarioId), actual);
	}

	/** Paso 2 «Cambiar contraseña»: vuelve a comprobar la actual, aplica la regla y guarda la nueva. */
	@Transactional(noRollbackFor = ExcepcionNegocio.class)
	public void cambiar(UUID usuarioId, String actual, String nueva) {
		Usuario usuario = usuario(usuarioId);
		comprobarActual(usuario, actual);
		// Primero «es la misma», que es lo que el usuario necesita saber si repite su contraseña.
		if (nueva != null && hashes.matches(nueva, usuario.getContrasenaHash())) {
			throw ExcepcionNegocio.invalido("La nueva contraseña debe ser diferente de la actual.");
		}
		PoliticaContrasena.problema(nueva).ifPresent(problema -> {
			throw ExcepcionNegocio.invalido(problema);
		});
		usuario.reemplazarContrasena(hashes.encode(nueva));
		usuarios.flush();
	}

	private Usuario usuario(UUID usuarioId) {
		return usuarios.findById(usuarioId)
				.filter(Usuario::puedeIniciarSesion)
				.orElseThrow(() -> new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida"));
	}

	private void comprobarActual(Usuario usuario, String actual) {
		Instant ahora = reloj.instant();
		if (usuario.bloqueadoEn(ahora)) {
			throw bloqueada(Duration.between(ahora, usuario.getBloqueadoHasta()));
		}
		if (actual == null || !hashes.matches(actual, usuario.getContrasenaHash())) {
			if (usuario.registrarFallo(ahora)) {
				throw bloqueada(Usuario.DURACION_BLOQUEO);
			}
			throw ExcepcionNegocio.invalido(CONTRASENA_INCORRECTA);
		}
	}

	private static ExcepcionNegocio bloqueada(Duration restante) {
		long minutos = Math.max(1, (restante.toSeconds() + 59) / 60);
		return new ExcepcionNegocio(HttpStatus.LOCKED,
				"Por seguridad, tu cuenta se bloqueó por varios intentos fallidos. Inténtalo de nuevo en " + minutos
						+ (minutos == 1 ? " minuto." : " minutos."));
	}
}
