package com.galiledu.usuarios.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** Estado de RF-1 y RF-2; nunca incluye el hash en toString(). */
public final class EstadoAcceso {
	private final String nombreUsuario;
	private final String hashContrasena;
	private final EstadoUsuario estado;
	private final int intentosFallidos;
	private final Instant bloqueadoHasta;
	private final Set<Rol> roles;

	public EstadoAcceso(String nombreUsuario, String hashContrasena, EstadoUsuario estado,
		int intentosFallidos, Instant bloqueadoHasta, Set<Rol> roles) {
		if (nombreUsuario == null || nombreUsuario.isBlank() || hashContrasena == null
			|| hashContrasena.isBlank() || intentosFallidos < 0 || roles == null || roles.isEmpty()) {
			throw new IllegalArgumentException("Estado de acceso inválido");
		}
		this.nombreUsuario = nombreUsuario;
		this.hashContrasena = hashContrasena;
		this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
		this.intentosFallidos = intentosFallidos;
		this.bloqueadoHasta = bloqueadoHasta;
		this.roles = Set.copyOf(roles);
	}

	public String nombreUsuario() {
		return nombreUsuario;
	}

	/** Solo para comprobar credenciales dentro de la capa de aplicación. */
	public String hashContrasena() {
		return hashContrasena;
	}

	public EstadoUsuario estado() {
		return estado;
	}

	public int intentosFallidos() {
		return intentosFallidos;
	}

	public Instant bloqueadoHasta() {
		return bloqueadoHasta;
	}

	public Set<Rol> roles() {
		return roles;
	}

	public boolean bloqueadoEn(Instant ahora) {
		return estado == EstadoUsuario.BLOQUEADO && bloqueadoHasta != null
			&& Objects.requireNonNull(ahora).isBefore(bloqueadoHasta);
	}

	public EstadoAcceso registrarFallo(Instant ahora) {
		Objects.requireNonNull(ahora, "El instante es obligatorio");
		int siguientes = (estado == EstadoUsuario.BLOQUEADO ? 0 : intentosFallidos) + 1;
		if (siguientes >= 5) {
			return new EstadoAcceso(nombreUsuario, hashContrasena, EstadoUsuario.BLOQUEADO,
				5, ahora.plusSeconds(15 * 60), roles);
		}
		return new EstadoAcceso(nombreUsuario, hashContrasena, EstadoUsuario.ACTIVO,
			siguientes, null, roles);
	}

	public EstadoAcceso registrarExito() {
		return new EstadoAcceso(nombreUsuario, hashContrasena, EstadoUsuario.ACTIVO,
			0, null, roles);
	}

	@Override
	public String toString() {
		return "EstadoAcceso[nombreUsuario=" + nombreUsuario + ", estado=" + estado
			+ ", intentosFallidos=" + intentosFallidos + "]";
	}
}
