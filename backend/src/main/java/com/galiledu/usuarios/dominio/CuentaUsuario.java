package com.galiledu.usuarios.dominio;

import java.util.Objects;
import java.util.Set;

/** Cuenta con identidad y transiciones de estado; no es un DTO ni una entidad JPA. */
public final class CuentaUsuario {
	private final DatosPersonales persona;
	private final String nombreUsuario;
	private final String hashContrasena;
	private final Set<Rol> roles;
	private final boolean activa;

	public CuentaUsuario(DatosPersonales persona, String nombreUsuario, String hashContrasena,
		Set<Rol> roles, boolean activa) {
		this.persona = Objects.requireNonNull(persona, "La persona es obligatoria");
		this.nombreUsuario = obligatorio(nombreUsuario, "El nombre de usuario es obligatorio");
		this.hashContrasena = obligatorio(hashContrasena, "El hash de contraseña es obligatorio");
		if (roles == null || roles.isEmpty()) {
			throw new IllegalArgumentException("La cuenta debe tener al menos un rol");
		}
		this.roles = Set.copyOf(roles);
		this.activa = activa;
	}

	public static CuentaUsuario nueva(DatosPersonales persona, String nombreUsuario,
		String hashInicial, Set<Rol> roles) {
		return new CuentaUsuario(persona, nombreUsuario, hashInicial, roles, true);
	}

	public DatosPersonales persona() {
		return persona;
	}

	public String nombreUsuario() {
		return nombreUsuario;
	}

	/** Solo para los servicios internos de autenticación y el futuro adaptador de datos. */
	public String hashContrasena() {
		return hashContrasena;
	}

	public Set<Rol> roles() {
		return roles;
	}

	public boolean activa() {
		return activa;
	}

	public boolean puedeUsarFuncionalidades() {
		return activa;
	}

	public CuentaUsuario desactivar() {
		return new CuentaUsuario(persona, nombreUsuario, hashContrasena, roles, false);
	}

	public CuentaUsuario reactivar() {
		return new CuentaUsuario(persona, nombreUsuario, hashContrasena, roles, true);
	}

	@Override
	public String toString() {
		return "CuentaUsuario[nombreUsuario=" + nombreUsuario + ", roles=" + roles
			+ ", activa=" + activa + "]";
	}

	private static String obligatorio(String valor, String mensaje) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException(mensaje);
		}
		return valor;
	}
}
