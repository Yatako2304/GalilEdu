package com.galiledu.usuarios.infraestructura.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;

@Component
public final class ServicioContrasenasBcrypt implements ServicioContrasenas {
	private final PasswordEncoder codificador;

	public ServicioContrasenasBcrypt(PasswordEncoder codificador) {
		this.codificador = codificador;
	}

	@Override
	public boolean coincide(String contrasena, String hash) {
		return codificador.matches(contrasena, hash);
	}

	@Override
	public String codificar(String contrasena) {
		return codificador.encode(contrasena);
	}
}
