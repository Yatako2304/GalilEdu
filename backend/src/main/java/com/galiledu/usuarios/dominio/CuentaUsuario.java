package com.galiledu.usuarios.dominio;

import java.util.Set;

/** Estado mínimo de la cuenta: permite varios roles y exige el primer cambio de clave. */
public record CuentaUsuario(
	DatosPersonales persona,
	String nombreUsuario,
	String hashContrasena,
	Set<Rol> roles,
	boolean activa,
	boolean cambioContrasenaObligatorio
) {
	public CuentaUsuario {
		if (persona == null) {
			throw new IllegalArgumentException("La persona es obligatoria");
		}
		if (nombreUsuario == null || nombreUsuario.isBlank()) {
			throw new IllegalArgumentException("El nombre de usuario es obligatorio");
		}
		if (hashContrasena == null || hashContrasena.isBlank()) {
			throw new IllegalArgumentException("El hash de contraseña es obligatorio");
		}
		if (roles == null || roles.isEmpty()) {
			throw new IllegalArgumentException("La cuenta debe tener al menos un rol");
		}
		roles = Set.copyOf(roles);
	}

	public static CuentaUsuario nueva(
		DatosPersonales persona, String nombreUsuario, String hashTemporal, Set<Rol> roles
	) {
		return new CuentaUsuario(persona, nombreUsuario, hashTemporal, roles, true, true);
	}

	public boolean puedeUsarFuncionalidades() {
		return activa && !cambioContrasenaObligatorio;
	}

	public CuentaUsuario desactivar() {
		return new CuentaUsuario(persona, nombreUsuario, hashContrasena, roles, false,
			cambioContrasenaObligatorio);
	}

	public CuentaUsuario reactivar() {
		return new CuentaUsuario(persona, nombreUsuario, hashContrasena, roles, true,
			cambioContrasenaObligatorio);
	}

	/** Llamar solo después de verificar la identidad y validar la nueva contraseña. */
	public CuentaUsuario completarCambioInicial(String nuevoHash) {
		if (!activa || !cambioContrasenaObligatorio) {
			throw new IllegalStateException("La cuenta no tiene un cambio inicial pendiente");
		}
		return new CuentaUsuario(persona, nombreUsuario, nuevoHash, roles, true, false);
	}

	@Override
	public String toString() {
		return "CuentaUsuario[nombreUsuario=" + nombreUsuario + ", roles=" + roles
			+ ", activa=" + activa + ", cambioContrasenaObligatorio="
			+ cambioContrasenaObligatorio + "]";
	}
}
