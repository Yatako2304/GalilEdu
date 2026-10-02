package com.galiledu.usuarios.aplicacion.puertos;

/** Evita que los casos de uso dependan de una librería concreta de hash de contraseñas. */
public interface ServicioContrasenas {
	boolean coincide(String contrasena, String hash);

	String codificar(String contrasena);
}
