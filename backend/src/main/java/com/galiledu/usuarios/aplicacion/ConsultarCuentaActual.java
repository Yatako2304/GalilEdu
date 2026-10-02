package com.galiledu.usuarios.aplicacion;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioCuentas;

/** Consulta los datos que puede mostrar la sesión de una cuenta activa. */
public class ConsultarCuentaActual {
	private final RepositorioCuentas cuentas;

	public ConsultarCuentaActual(RepositorioCuentas cuentas) {
		this.cuentas = Objects.requireNonNull(cuentas);
	}

	public Optional<CuentaActual> ejecutar(String nombreUsuario) {
		return cuentas.buscarPorNombreUsuario(nombreUsuario)
			.filter(cuenta -> cuenta.activa())
			.map(cuenta -> new CuentaActual(cuenta.nombreUsuario(),
				cuenta.roles().stream().map(rol -> rol.codigo()).sorted().toList()));
	}

	public record CuentaActual(String nombreUsuario, List<String> roles) {}
}
