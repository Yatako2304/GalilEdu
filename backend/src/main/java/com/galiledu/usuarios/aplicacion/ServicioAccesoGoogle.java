package com.galiledu.usuarios.aplicacion;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.usuarios.aplicacion.CuentasGoogle.CuentaGoogle;
import com.galiledu.usuarios.aplicacion.Dtos.ConfiguracionAccesoDto;
import com.galiledu.usuarios.aplicacion.Dtos.MetodosAcceso;
import com.galiledu.usuarios.aplicacion.Dtos.RespuestaLogin;
import com.galiledu.usuarios.dominio.Usuario;
import com.galiledu.usuarios.infraestructura.ConfiguracionAcceso;
import com.galiledu.usuarios.infraestructura.UsuarioRepositorio;

/**
 * HU-2 (el administrador activa o desactiva Google para el colegio) y HU-3 (inicio de sesión con Google). Solo entra
 * la cuenta cuyo correo registrado en GalilEdu (personas.persona.correo, sin distinguir mayúsculas) es el de la cuenta
 * de Google, con el correo verificado por Google. Usuario y contraseña siguen funcionando siempre.
 */
@Service
public class ServicioAccesoGoogle {
	static final String NO_HABILITADO = "El inicio de sesión con Google no está habilitado en el colegio.";
	static final String SIN_CUENTA = "No se encontró una cuenta de GalilEdu asociada a este correo de Google.";
	static final String CORREO_NO_VERIFICADO = "Google no ha verificado el correo de esta cuenta.";
	static final String SIN_CONFIGURAR = "Falta configurar GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET en el servidor.";

	private final ConfiguracionAcceso configuracion;
	private final CuentasGoogle google;
	private final UsuarioRepositorio usuarios;
	private final ServicioAutenticacion autenticacion;

	public ServicioAccesoGoogle(ConfiguracionAcceso configuracion, CuentasGoogle google, UsuarioRepositorio usuarios,
			ServicioAutenticacion autenticacion) {
		this.configuracion = configuracion;
		this.google = google;
		this.usuarios = usuarios;
		this.autenticacion = autenticacion;
	}

	/** El botón se muestra si el administrador lo activó y el servidor tiene las llaves de Google. */
	@Transactional(readOnly = true)
	public MetodosAcceso metodos() {
		boolean disponible = disponible();
		return new MetodosAcceso(disponible, disponible ? google.idCliente() : null);
	}

	@Transactional(readOnly = true)
	public ConfiguracionAccesoDto configuracion() {
		return new ConfiguracionAccesoDto(configuracion.googleHabilitado(), google.configurado());
	}

	/** HU-2. Activar exige las llaves de Google en el servidor (409); desactivar siempre se puede. */
	@Transactional
	public ConfiguracionAccesoDto cambiarGoogle(boolean habilitar, UUID administradorId) {
		if (habilitar && !google.configurado()) {
			throw new ExcepcionNegocio(HttpStatus.CONFLICT, SIN_CONFIGURAR);
		}
		configuracion.cambiarGoogle(habilitar, administradorId);
		return configuracion();
	}

	/** HU-3. El bloqueo y la cuenta desactivada se tratan igual que en el inicio con contraseña. */
	@Transactional(noRollbackFor = ExcepcionNegocio.class)
	public RespuestaLogin iniciarSesion(String codigo) {
		if (!disponible()) {
			throw new ExcepcionNegocio(HttpStatus.FORBIDDEN, NO_HABILITADO);
		}
		CuentaGoogle cuenta = google.cuentaDe(codigo);
		if (!cuenta.correoVerificado()) {
			throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, CORREO_NO_VERIFICADO);
		}
		Usuario usuario = usuarios.findByPersonaCorreoIgnoreCaseAndEliminadoFalse(cuenta.correo().strip())
				.orElseThrow(() -> new ExcepcionNegocio(HttpStatus.NOT_FOUND, SIN_CUENTA));
		return autenticacion.iniciarSesionVerificada(usuario);
	}

	private boolean disponible() {
		return google.configurado() && configuracion.googleHabilitado();
	}
}
