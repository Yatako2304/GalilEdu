package com.galiledu.usuarios.infraestructura.seguridad;

import org.springframework.security.authentication.LockedException;

/** Conserva el tiempo restante de RF-2 hasta la respuesta HTTP. */
public final class CuentaBloqueadaException extends LockedException {
	private final long segundosRestantes;

	public CuentaBloqueadaException(long segundosRestantes) {
		super("Cuenta bloqueada temporalmente");
		this.segundosRestantes = segundosRestantes;
	}

	public long segundosRestantes() {
		return segundosRestantes;
	}
}
