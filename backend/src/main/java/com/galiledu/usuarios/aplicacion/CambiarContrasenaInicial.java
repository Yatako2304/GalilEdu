package com.galiledu.usuarios.aplicacion;

import java.util.Objects;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioCuentas;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.dominio.CuentaUsuario;

/** Caso de uso del primer cambio obligatorio, acordado para la contraseña temporal. */
public final class CambiarContrasenaInicial {
	private final RepositorioCuentas cuentas;
	private final ServicioContrasenas contrasenas;

	public CambiarContrasenaInicial(RepositorioCuentas cuentas, ServicioContrasenas contrasenas) {
		this.cuentas = Objects.requireNonNull(cuentas);
		this.contrasenas = Objects.requireNonNull(contrasenas);
	}

	public void ejecutar(String nombreUsuario, String contrasenaTemporal, String nuevaContrasena) {
		if (nombreUsuario == null || nombreUsuario.isBlank()
			|| contrasenaTemporal == null || contrasenaTemporal.isBlank()) {
			throw new IllegalArgumentException("Credenciales inválidas");
		}
		if (nuevaContrasena == null || nuevaContrasena.isBlank()) {
			throw new IllegalArgumentException("La nueva contraseña es obligatoria");
		}

		CuentaUsuario cuenta = cuentas.buscarPorNombreUsuario(nombreUsuario)
			.orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
		if (!cuenta.activa() || !cuenta.cambioContrasenaObligatorio()) {
			throw new IllegalStateException("La cuenta no tiene un cambio inicial pendiente");
		}
		if (!contrasenas.coincide(contrasenaTemporal, cuenta.hashContrasena())) {
			throw new IllegalArgumentException("Credenciales inválidas");
		}
		if (contrasenas.coincide(nuevaContrasena, cuenta.hashContrasena())) {
			throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la temporal");
		}

		cuentas.guardar(cuenta.completarCambioInicial(contrasenas.codificar(nuevaContrasena)));
	}
}
