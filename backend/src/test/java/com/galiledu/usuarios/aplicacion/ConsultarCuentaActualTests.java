package com.galiledu.usuarios.aplicacion;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioCuentas;
import com.galiledu.usuarios.dominio.CuentaUsuario;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.Rol;
import com.galiledu.usuarios.dominio.TipoDocumento;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarCuentaActualTests {
	private final RepositorioEnMemoria cuentas = new RepositorioEnMemoria();
	private final ConsultarCuentaActual consulta = new ConsultarCuentaActual(cuentas);

	@Test
	void presentaRolesOrdenadosSinExponerCredenciales() {
		cuentas.guardar(cuenta());

		var resultado = consulta.ejecutar("U20260001").orElseThrow();

		assertThat(resultado.nombreUsuario()).isEqualTo("U20260001");
		assertThat(resultado.roles()).containsExactly("APODERADO", "DOCENTE");
	}

	@Test
	void omiteCuentasInactivasOInexistentes() {
		cuentas.guardar(cuenta().desactivar());

		assertThat(consulta.ejecutar("U20260001")).isEmpty();
		assertThat(consulta.ejecutar("desconocido")).isEmpty();
	}

	private static CuentaUsuario cuenta() {
		var persona = new DatosPersonales("María", "López", "Quispe", TipoDocumento.DNI,
			"12345678", "maria@example.com", "987654321");
		return CuentaUsuario.nueva(persona, "U20260001", "hash-privado",
			Set.of(new Rol("DOCENTE"), new Rol("APODERADO")));
	}

	private static final class RepositorioEnMemoria implements RepositorioCuentas {
		private CuentaUsuario cuenta;

		@Override
		public Optional<CuentaUsuario> buscarPorNombreUsuario(String nombreUsuario) {
			return cuenta != null && cuenta.nombreUsuario().equals(nombreUsuario)
				? Optional.of(cuenta) : Optional.empty();
		}

		@Override
		public void guardar(CuentaUsuario cuenta) {
			this.cuenta = cuenta;
		}
	}
}
