package com.galiledu.usuarios.dominio;

import java.time.Instant;
import java.util.Set;

/** Estado usado por RF-1 y RF-2; el bloqueo persiste entre solicitudes. */
public record EstadoAcceso(
	String nombreUsuario,
	String hashContrasena,
	EstadoUsuario estado,
	int intentosFallidos,
	Instant bloqueadoHasta,
	boolean cambioContrasenaObligatorio,
	Set<Rol> roles
) {
	public EstadoAcceso {
		if (nombreUsuario == null || nombreUsuario.isBlank() || hashContrasena == null
			|| hashContrasena.isBlank() || estado == null || intentosFallidos < 0
			|| roles == null || roles.isEmpty()) {
			throw new IllegalArgumentException("Estado de acceso inválido");
		}
		roles = Set.copyOf(roles);
	}

	public boolean bloqueadoEn(Instant ahora) {
		return estado == EstadoUsuario.BLOQUEADO && bloqueadoHasta != null
			&& ahora.isBefore(bloqueadoHasta);
	}

	public EstadoAcceso registrarFallo(Instant ahora) {
		int siguientes = (estado == EstadoUsuario.BLOQUEADO ? 0 : intentosFallidos) + 1;
		if (siguientes >= 5) {
			return new EstadoAcceso(nombreUsuario, hashContrasena, EstadoUsuario.BLOQUEADO,
				5, ahora.plusSeconds(15 * 60), cambioContrasenaObligatorio, roles);
		}
		return new EstadoAcceso(nombreUsuario, hashContrasena, EstadoUsuario.ACTIVO,
			siguientes, null, cambioContrasenaObligatorio, roles);
	}

	public EstadoAcceso registrarExito() {
		return new EstadoAcceso(nombreUsuario, hashContrasena, EstadoUsuario.ACTIVO,
			0, null, cambioContrasenaObligatorio, roles);
	}
}
