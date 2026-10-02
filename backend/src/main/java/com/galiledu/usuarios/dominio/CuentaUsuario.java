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
	private final boolean cambioContrasenaObligatorio;

	public CuentaUsuario(DatosPersonales persona, String nombreUsuario, String hashContrasena,
		Set<Rol> roles, boolean activa, boolean cambioContrasenaObligatorio) {
		this.persona = Objects.requireNonNull(persona, "La persona es obligatoria");
		this.nombreUsuario = obligatorio(nombreUsuario, "El nombre de usuario es obligatorio");
		this.hashContrasena = obligatorio(hashContrasena, "El hash de contraseña es obligatorio");
		if (roles == null || roles.isEmpty()) {
			throw new IllegalArgumentException("La cuenta debe tener al menos un rol");
		}
		this.roles = Set.copyOf(roles);
		this.activa = activa;
		this.cambioContrasenaObligatorio = cambioContrasenaObligatorio;
	}

	public static CuentaUsuario nueva(DatosPersonales persona, String nombreUsuario,
		String hashTemporal, Set<Rol> roles) {
		return new CuentaUsuario(persona, nombreUsuario, hashTemporal, roles, true, true);
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

	public boolean cambioContrasenaObligatorio() {
		return cambioContrasenaObligatorio;
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

	private static String obligatorio(String valor, String mensaje) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException(mensaje);
		}
		return valor;
	}
}
