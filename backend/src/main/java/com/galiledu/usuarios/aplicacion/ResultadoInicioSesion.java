package com.galiledu.usuarios.aplicacion;

import java.util.Set;

import com.galiledu.usuarios.dominio.Rol;

public record ResultadoInicioSesion(
	Estado estado,
	String nombreUsuario,
	Set<Rol> roles,
	boolean cambioContrasenaObligatorio,
	long segundosRestantesBloqueo
) {
	public enum Estado { EXITOSO, CREDENCIALES_INVALIDAS, BLOQUEADO }

	public static ResultadoInicioSesion exitoso(String nombreUsuario, Set<Rol> roles, boolean cambio) {
		return new ResultadoInicioSesion(Estado.EXITOSO, nombreUsuario, Set.copyOf(roles), cambio, 0);
	}

	public static ResultadoInicioSesion invalido() {
		return new ResultadoInicioSesion(Estado.CREDENCIALES_INVALIDAS, null, Set.of(), false, 0);
	}

	public static ResultadoInicioSesion bloqueado(long segundos) {
		return new ResultadoInicioSesion(Estado.BLOQUEADO, null, Set.of(), false, segundos);
	}
}
