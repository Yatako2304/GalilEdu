package com.galiledu.usuarios.aplicacion;

import java.util.Objects;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioCredenciales;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-5 / RF-11: cada cuenta autenticada cambia únicamente su propia clave. */
@Service
public class CambiarContrasena {
	private final RepositorioCredenciales credenciales;
	private final ServicioContrasenas contrasenas;

	public CambiarContrasena(RepositorioCredenciales credenciales, ServicioContrasenas contrasenas) {
		this.credenciales = Objects.requireNonNull(credenciales);
		this.contrasenas = Objects.requireNonNull(contrasenas);
	}

	@Transactional
	public void ejecutar(String nombreUsuario, String contrasenaActual, String nuevaContrasena) {
		if (nombreUsuario == null || nombreUsuario.isBlank()
			|| contrasenaActual == null || contrasenaActual.isBlank()
			|| nuevaContrasena == null || nuevaContrasena.isBlank()) {
			throw new IllegalArgumentException("La contraseña actual y la nueva son obligatorias");
		}
		String hashActual = credenciales.buscarHashActivoParaActualizar(nombreUsuario)
			.orElseThrow(() -> new IllegalArgumentException("No se pudo cambiar la contraseña"));
		if (!contrasenas.coincide(contrasenaActual, hashActual)) {
			throw new IllegalArgumentException("La contraseña actual es incorrecta");
		}
		if (!credenciales.actualizarHash(nombreUsuario, hashActual, contrasenas.codificar(nuevaContrasena))) {
			throw new IllegalStateException("No se pudo actualizar la contraseña");
		}
	}
}
