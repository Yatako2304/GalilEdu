package com.galiledu.usuarios.dominio;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaUsuarioTests {
	private static DatosPersonales persona() {
		return new DatosPersonales("María", "López", "Quispe", TipoDocumento.DNI,
			"12345678", "maria@example.com", "987654321");
	}

	@Test
	void nuevaCuentaAdmiteVariosRolesYPuedeUsarseSinCambioInicial() {
		CuentaUsuario cuenta = CuentaUsuario.nueva(persona(), "U20260001", "hash-inicial",
			Set.of(new Rol("DOCENTE"), new Rol("APODERADO")));

		assertThat(cuenta.roles()).hasSize(2);
		assertThat(cuenta.activa()).isTrue();
		assertThat(cuenta.puedeUsarFuncionalidades()).isTrue();
		assertThat(cuenta.hashContrasena()).isEqualTo("hash-inicial");
	}

	@Test
	void noPermiteCrearCuentaSinRoles() {
		assertThatThrownBy(() -> CuentaUsuario.nueva(persona(), "U20260001", "hash", Set.of()))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void desactivarImpideAccesoSinPerderLaCuentaNiSusRoles() {
		CuentaUsuario activa = CuentaUsuario.nueva(persona(), "U20260001", "hash-secreto",
			Set.of(new Rol("DOCENTE"), new Rol("APODERADO")));
		CuentaUsuario inactiva = activa.desactivar();

		assertThat(inactiva.puedeUsarFuncionalidades()).isFalse();
		assertThat(inactiva.roles()).containsExactlyInAnyOrderElementsOf(activa.roles());
		assertThat(inactiva.reactivar().puedeUsarFuncionalidades()).isTrue();
		assertThat(inactiva.toString()).doesNotContain("hash-secreto");
	}
}
