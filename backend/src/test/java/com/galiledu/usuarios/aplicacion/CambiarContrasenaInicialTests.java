package com.galiledu.usuarios.aplicacion;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioCuentas;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.dominio.CuentaUsuario;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.Rol;
import com.galiledu.usuarios.dominio.TipoDocumento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CambiarContrasenaInicialTests {
	private final RepositorioEnMemoria cuentas = new RepositorioEnMemoria();
	private final ServicioContrasenas contrasenas = new ContrasenasDePrueba();
	private final CambiarContrasenaInicial casoDeUso =
		new CambiarContrasenaInicial(cuentas, contrasenas);

	private static CuentaUsuario cuentaNueva() {
		DatosPersonales persona = new DatosPersonales("María", "López", TipoDocumento.DNI,
			"12345678", "maria@example.com", "987654321");
		return CuentaUsuario.nueva(persona, "U20260001", "hash:temporal",
			Set.of(new Rol("DOCENTE"), new Rol("APODERADO")));
	}

	@Test
	void cambiaLaContrasenaTemporalYConservaLosRoles() {
		cuentas.guardar(cuentaNueva());

		casoDeUso.ejecutar("U20260001", "temporal", "nueva-clave");

		CuentaUsuario actualizada = cuentas.buscarPorNombreUsuario("U20260001").orElseThrow();
		assertThat(actualizada.hashContrasena()).isEqualTo("hash:nueva-clave");
		assertThat(actualizada.cambioContrasenaObligatorio()).isFalse();
		assertThat(actualizada.puedeUsarFuncionalidades()).isTrue();
		assertThat(actualizada.roles()).hasSize(2);
	}

	@Test
	void rechazaLaContrasenaTemporalIncorrectaSinGuardarCambios() {
		cuentas.guardar(cuentaNueva());

		assertThatThrownBy(() -> casoDeUso.ejecutar("U20260001", "erronea", "nueva-clave"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(cuentas.buscarPorNombreUsuario("U20260001").orElseThrow()
			.cambioContrasenaObligatorio()).isTrue();
	}

	@Test
	void impideReutilizarLaContrasenaTemporal() {
		cuentas.guardar(cuentaNueva());

		assertThatThrownBy(() -> casoDeUso.ejecutar("U20260001", "temporal", "temporal"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(cuentas.buscarPorNombreUsuario("U20260001").orElseThrow()
			.hashContrasena()).isEqualTo("hash:temporal");
	}

	@Test
	void rechazaCuentasInactivasYCambiosYaCompletados() {
		cuentas.guardar(cuentaNueva().desactivar());
		assertThatThrownBy(() -> casoDeUso.ejecutar("U20260001", "temporal", "nueva-clave"))
			.isInstanceOf(IllegalStateException.class);

		cuentas.guardar(cuentaNueva().completarCambioInicial("hash:definitiva"));
		assertThatThrownBy(() -> casoDeUso.ejecutar("U20260001", "temporal", "nueva-clave"))
			.isInstanceOf(IllegalStateException.class);
	}

	private static final class RepositorioEnMemoria implements RepositorioCuentas {
		private final Map<String, CuentaUsuario> datos = new HashMap<>();

		@Override
		public Optional<CuentaUsuario> buscarPorNombreUsuario(String nombreUsuario) {
			return Optional.ofNullable(datos.get(nombreUsuario));
		}

		@Override
		public void guardar(CuentaUsuario cuenta) {
			datos.put(cuenta.nombreUsuario(), cuenta);
		}
	}

	/** No se usa en producción: permite probar la coordinación sin depender de bcrypt. */
	private static final class ContrasenasDePrueba implements ServicioContrasenas {
		@Override
		public boolean coincide(String contrasena, String hash) {
			return ("hash:" + contrasena).equals(hash);
		}

		@Override
		public String codificar(String contrasena) {
			return "hash:" + contrasena;
		}
	}
}
