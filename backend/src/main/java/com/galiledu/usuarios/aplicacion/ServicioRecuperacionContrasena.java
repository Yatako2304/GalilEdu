package com.galiledu.usuarios.aplicacion;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.usuarios.dominio.PoliticaContrasena;
import com.galiledu.usuarios.dominio.Usuario;
import com.galiledu.usuarios.infraestructura.CodigosRecuperacion;
import com.galiledu.usuarios.infraestructura.CodigosRecuperacion.Codigo;
import com.galiledu.usuarios.infraestructura.NotificadorCredenciales;
import com.galiledu.usuarios.infraestructura.UsuarioRepositorio;

/**
 * HU-4 (Figma › Autenticación › Restablecer contraseña): el usuario escribe su correo, recibe un código de 6 dígitos,
 * lo valida y elige una contraseña nueva. Solo vale el código más reciente, por {@link #VIGENCIA} y con
 * {@link #MAX_INTENTOS} intentos; se puede pedir otro cada {@link #ESPERA_REENVIO}.
 */
@Service
public class ServicioRecuperacionContrasena {
	public static final Duration VIGENCIA = Duration.ofMinutes(10);
	public static final Duration ESPERA_REENVIO = Duration.ofSeconds(60);
	public static final int MAX_INTENTOS = 5;
	static final String SIN_CUENTA = "El correo no está asociado a ninguna cuenta.";
	static final String CODIGO_INCORRECTO = "Código de verificación incorrecto.";
	static final String CODIGO_VENCIDO = "El código venció. Solicita uno nuevo con «Reenviar código».";
	static final String SIN_INTENTOS = "Superaste los intentos permitidos. Solicita un nuevo código con «Reenviar código».";
	private static final SecureRandom AZAR = new SecureRandom();

	private final UsuarioRepositorio usuarios;
	private final CodigosRecuperacion codigos;
	private final NotificadorCredenciales notificador;
	private final PasswordEncoder hashes;
	private final Clock reloj;

	public ServicioRecuperacionContrasena(UsuarioRepositorio usuarios, CodigosRecuperacion codigos,
			NotificadorCredenciales notificador, PasswordEncoder hashes, Clock reloj) {
		this.usuarios = usuarios;
		this.codigos = codigos;
		this.notificador = notificador;
		this.hashes = hashes;
		this.reloj = reloj;
	}

	/** Paso 1 y «Reenviar código»: genera un código nuevo (anula el anterior) y lo envía al correo. */
	@Transactional
	public void solicitar(String correo) {
		Usuario usuario = cuentaDe(correo);
		Instant ahora = reloj.instant();
		codigos.ultimo(usuario.getId()).ifPresent(anterior -> {
			Duration pasado = Duration.between(anterior.creadoEn(), ahora);
			if (pasado.compareTo(ESPERA_REENVIO) < 0) {
				long segundos = Math.max(1, ESPERA_REENVIO.minus(pasado).toSeconds());
				throw new ExcepcionNegocio(HttpStatus.TOO_MANY_REQUESTS,
						"Espera " + segundos + " segundos para pedir otro código.");
			}
		});
		String codigo = "%06d".formatted(AZAR.nextInt(1_000_000));
		codigos.guardar(usuario.getId(), hashes.encode(codigo), ahora, ahora.plus(VIGENCIA));
		var persona = usuario.getPersona();
		if (!notificador.enviarCodigoRecuperacion(persona.getCorreo(), persona.getNombres(), codigo,
				VIGENCIA.toMinutes())) {
			// Sin correo enviado no hay código válido: se deshace lo guardado.
			throw new ExcepcionNegocio(HttpStatus.SERVICE_UNAVAILABLE,
					"No pudimos enviar el código a tu correo. Inténtalo nuevamente en unos minutos.");
		}
	}

	/** Paso 2 «Validar código». Un intento fallido cuenta aunque la respuesta sea un error (noRollbackFor). */
	@Transactional(noRollbackFor = ExcepcionNegocio.class)
	public void verificar(String correo, String codigo) {
		codigoValido(cuentaDe(correo), codigo);
	}

	/** Paso 3 «Restablecer contraseña»: vuelve a comprobar el código, guarda la contraseña y lo marca usado. */
	@Transactional(noRollbackFor = ExcepcionNegocio.class)
	public void restablecer(String correo, String codigo, String contrasena) {
		Usuario usuario = cuentaDe(correo);
		Codigo vigente = codigoValido(usuario, codigo);
		PoliticaContrasena.problema(contrasena).ifPresent(problema -> {
			throw ExcepcionNegocio.invalido(problema);
		});
		// reemplazarContrasena también quita el bloqueo por intentos fallidos de inicio de sesión.
		usuario.reemplazarContrasena(hashes.encode(contrasena));
		codigos.marcarUsado(vigente.id(), reloj.instant());
	}

	private Usuario cuentaDe(String correo) {
		Usuario usuario = usuarios.findByPersonaCorreoIgnoreCaseAndEliminadoFalse(correo.strip())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado(SIN_CUENTA));
		if (!usuario.puedeIniciarSesion()) {
			throw ExcepcionNegocio.conflicto(
					"Tu cuenta está desactivada. Comunícate con el administrador del colegio.");
		}
		return usuario;
	}

	private Codigo codigoValido(Usuario usuario, String codigo) {
		Codigo ultimo = codigos.ultimo(usuario.getId())
				.filter(c -> c.usadoEn() == null)
				.orElseThrow(() -> ExcepcionNegocio.invalido(CODIGO_VENCIDO));
		if (ultimo.intentos() >= MAX_INTENTOS) {
			throw ExcepcionNegocio.invalido(SIN_INTENTOS);
		}
		if (!reloj.instant().isBefore(ultimo.expiraEn())) {
			throw ExcepcionNegocio.invalido(CODIGO_VENCIDO);
		}
		String limpio = codigo == null ? "" : codigo.strip();
		if (!limpio.matches("\\d{6}") || !hashes.matches(limpio, ultimo.codigoHash())) {
			codigos.sumarIntento(ultimo.id());
			throw ExcepcionNegocio.invalido(ultimo.intentos() + 1 >= MAX_INTENTOS ? SIN_INTENTOS : CODIGO_INCORRECTO);
		}
		return ultimo;
	}
}
