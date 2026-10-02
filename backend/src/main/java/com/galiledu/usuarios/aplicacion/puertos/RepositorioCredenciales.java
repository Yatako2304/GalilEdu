package com.galiledu.usuarios.aplicacion.puertos;

import java.util.Optional;

/** Acceso a las credenciales de la cuenta autenticada, sin exponerlas por HTTP. */
public interface RepositorioCredenciales {
	Optional<String> buscarHashActivoParaActualizar(String nombreUsuario);

	boolean actualizarHash(String nombreUsuario, String hashAnterior, String hashNuevo);
}
