package com.galiledu.usuarios.aplicacion.puertos;

import java.util.Optional;

import com.galiledu.usuarios.dominio.EstadoAcceso;

public interface RepositorioAccesos {
	/** El adaptador definitivo deberá serializar la comprobación y actualización de intentos. */
	Optional<EstadoAcceso> buscarParaAutenticacion(String nombreUsuario);

	void guardarEstado(EstadoAcceso acceso);
}
