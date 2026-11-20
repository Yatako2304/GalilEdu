package com.galiledu.usuarios.aplicacion;

/**
 * HU-3: lo que GalilEdu necesita de Google. El frontend obtiene un código de autorización con la ventana de Google y
 * aquí se canjea por la cuenta que inició sesión. La implementación real es {@code CuentasGoogleOAuth}.
 */
public interface CuentasGoogle {

	/** Hay ID de cliente y clave secreta de Google configurados en el servidor. */
	boolean configurado();

	/** ID de cliente OAuth (público: lo usa el botón del frontend). Vacío si no está configurado. */
	String idCliente();

	/**
	 * Canjea el código de autorización y devuelve la cuenta verificada por Google. Lanza ExcepcionNegocio 401 si
	 * Google rechaza el código o la respuesta no es válida.
	 */
	CuentaGoogle cuentaDe(String codigo);

	record CuentaGoogle(String correo, boolean correoVerificado) {
	}
}
