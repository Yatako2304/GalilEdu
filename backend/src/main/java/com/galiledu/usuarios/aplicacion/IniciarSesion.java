package com.galiledu.usuarios.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioAccesos;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.dominio.EstadoAcceso;
import com.galiledu.usuarios.dominio.EstadoUsuario;

public class IniciarSesion {
	private final RepositorioAccesos accesos;
	private final ServicioContrasenas contrasenas;
	private final Clock reloj;
	private final String hashFicticio;

	public IniciarSesion(RepositorioAccesos accesos, ServicioContrasenas contrasenas, Clock reloj) {
		this.accesos = accesos;
		this.contrasenas = contrasenas;
		this.reloj = reloj;
		this.hashFicticio = contrasenas.codificar("cuenta-inexistente");
	}

	public ResultadoInicioSesion ejecutar(String nombreUsuario, String contrasena) {
		if (nombreUsuario == null || nombreUsuario.isBlank() || contrasena == null || contrasena.isBlank()) {
			return ResultadoInicioSesion.invalido();
		}
		var encontrado = accesos.buscarParaAutenticacion(nombreUsuario);
		if (encontrado.isEmpty()) {
			contrasenas.coincide(contrasena, hashFicticio);
			return ResultadoInicioSesion.invalido();
		}
		EstadoAcceso acceso = encontrado.orElseThrow();
		if (acceso.estado() == EstadoUsuario.INACTIVO) {
			return ResultadoInicioSesion.invalido();
		}
		Instant ahora = reloj.instant();
		if (acceso.bloqueadoEn(ahora)) {
			long segundos = ChronoUnit.SECONDS.between(ahora, acceso.bloqueadoHasta());
			return ResultadoInicioSesion.bloqueado(Math.max(1, segundos));
		}
		if (!contrasenas.coincide(contrasena, acceso.hashContrasena())) {
			EstadoAcceso actualizado = acceso.registrarFallo(ahora);
			accesos.guardarEstado(actualizado);
			return actualizado.estado() == EstadoUsuario.BLOQUEADO
				? ResultadoInicioSesion.bloqueado(15 * 60)
				: ResultadoInicioSesion.invalido();
		}
		EstadoAcceso actualizado = acceso.registrarExito();
		accesos.guardarEstado(actualizado);
		return ResultadoInicioSesion.exitoso(actualizado.nombreUsuario(), actualizado.roles(),
			actualizado.cambioContrasenaObligatorio());
	}
}
