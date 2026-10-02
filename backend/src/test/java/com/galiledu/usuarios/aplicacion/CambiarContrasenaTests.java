package com.galiledu.usuarios.aplicacion;

import java.util.Optional;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioCredenciales;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CambiarContrasenaTests {
	private final CredencialesEnMemoria credenciales = new CredencialesEnMemoria();
	private final CambiarContrasena cambiar = new CambiarContrasena(credenciales, new ContrasenasDePrueba());

	@Test
	void cambiaSoloLaCuentaIdentificadaTrasVerificarClaveActual() {
		cambiar.ejecutar("apoderado", "anterior", "nueva");
		assertThat(credenciales.hash).isEqualTo("hash:nueva");
		assertThat(credenciales.usuarioActualizado).isEqualTo("apoderado");
	}

	@Test
	void claveActualIncorrectaNoModificaElHash() {
		assertThatThrownBy(() -> cambiar.ejecutar("apoderado", "incorrecta", "nueva"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(credenciales.hash).isEqualTo("hash:anterior");
	}

	@Test
	void noAdmiteCuentaAjenaONuevaClaveVacia() {
		assertThatThrownBy(() -> cambiar.ejecutar("otro", "anterior", "nueva"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> cambiar.ejecutar("apoderado", "anterior", " "))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(credenciales.hash).isEqualTo("hash:anterior");
	}


	private static final class CredencialesEnMemoria implements RepositorioCredenciales {
		private String hash = "hash:anterior";
		private String usuarioActualizado;

		@Override
		public Optional<String> buscarHashActivoParaActualizar(String nombreUsuario) {
			return "apoderado".equals(nombreUsuario) ? Optional.of(hash) : Optional.empty();
		}

		@Override
		public boolean actualizarHash(String nombreUsuario, String hashAnterior, String hashNuevo) {
			if (!"apoderado".equals(nombreUsuario) || !hash.equals(hashAnterior)) return false;
			hash = hashNuevo;
			usuarioActualizado = nombreUsuario;
			return true;
		}
	}

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
