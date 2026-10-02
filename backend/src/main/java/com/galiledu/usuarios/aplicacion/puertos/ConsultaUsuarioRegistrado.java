package com.galiledu.usuarios.aplicacion.puertos;

import java.util.Optional;

import com.galiledu.usuarios.aplicacion.UsuarioRegistrado;

/** Lectura de seguridad.usuario y sus roles; no valida contraseñas. */
public interface ConsultaUsuarioRegistrado {
	Optional<UsuarioRegistrado> buscarPorCorreo(String correo);
}
