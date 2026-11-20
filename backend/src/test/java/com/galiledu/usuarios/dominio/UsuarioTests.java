package com.galiledu.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.personas.dominio.Persona;

class UsuarioTests {
	private final Instant ahora = Instant.parse("2026-10-19T15:00:00Z");

	private Usuario nuevo() {
		return new Usuario(new Persona(), "U20260001", "hash", Set.of(Rol.DOCENTE));
	}

	@DisplayName("Se bloquea quince minutos al quinto intento fallido")
	@Test
	void seBloqueaQuinceMinutosAlQuintoIntentoFallido() {
		Usuario u = nuevo();
		for (int i = 1; i < Usuario.MAX_INTENTOS; i++) {
			assertThat(u.registrarFallo(ahora)).isFalse();
		}
		assertThat(u.registrarFallo(ahora)).isTrue();
		assertThat(u.bloqueadoEn(ahora.plusSeconds(14 * 60))).isTrue();
		assertThat(u.bloqueadoEn(ahora.plusSeconds(15 * 60))).isFalse();
	}

	@DisplayName("Un acceso exitoso reinicia los intentos")
	@Test
	void unAccesoExitosoReiniciaLosIntentos() {
		Usuario u = nuevo();
		u.registrarFallo(ahora);
		u.registrarFallo(ahora);
		u.registrarAcceso(ahora);
		for (int i = 1; i < Usuario.MAX_INTENTOS; i++) {
			assertThat(u.registrarFallo(ahora)).isFalse();
		}
	}

	@DisplayName("Desactivado o eliminado no puede iniciar sesión")
	@Test
	void desactivadoOEliminadoNoPuedeIniciarSesion() {
		Usuario u = nuevo();
		assertThat(u.puedeIniciarSesion()).isTrue();
		u.desactivar();
		assertThat(u.puedeIniciarSesion()).isFalse();
		u.reactivar();
		assertThat(u.puedeIniciarSesion()).isTrue();
		u.eliminar();
		assertThat(u.puedeIniciarSesion()).isFalse();
	}
}
