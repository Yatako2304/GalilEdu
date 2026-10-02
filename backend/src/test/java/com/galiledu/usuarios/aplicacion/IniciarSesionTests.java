package com.galiledu.usuarios.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioAccesos;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.dominio.EstadoAcceso;
import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Rol;

import static org.assertj.core.api.Assertions.assertThat;

class IniciarSesionTests {
	private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
	private final ServicioContrasenas contrasenas = new ContrasenasDePrueba();
	private final Instant ahora = Instant.parse("2026-10-01T12:00:00Z");
	private final IniciarSesion iniciar = new IniciarSesion(repositorio, contrasenas,
		Clock.fixed(ahora, ZoneOffset.UTC));

	@Test
	void aceptaCuentaActivaYConservaCambioObligatorio() {
		repositorio.guardarEstado(cuenta(EstadoUsuario.ACTIVO));

		ResultadoInicioSesion resultado = iniciar.ejecutar("U20260001", "temporal");

		assertThat(resultado.estado()).isEqualTo(ResultadoInicioSesion.Estado.EXITOSO);
		assertThat(resultado.cambioContrasenaObligatorio()).isTrue();
		assertThat(resultado.roles()).hasSize(2);
		assertThat(repositorio.buscarParaAutenticacion("U20260001").orElseThrow()
			.intentosFallidos()).isZero();
	}

	@Test
	void bloqueaQuinceMinutosTrasCincoFallosConsecutivos() {
		repositorio.guardarEstado(cuenta(EstadoUsuario.ACTIVO));
		for (int intento = 1; intento <= 4; intento++) {
			assertThat(iniciar.ejecutar("U20260001", "erronea").estado())
				.isEqualTo(ResultadoInicioSesion.Estado.CREDENCIALES_INVALIDAS);
		}
		ResultadoInicioSesion quinto = iniciar.ejecutar("U20260001", "erronea");
		assertThat(quinto.estado()).isEqualTo(ResultadoInicioSesion.Estado.BLOQUEADO);
		assertThat(quinto.segundosRestantesBloqueo()).isEqualTo(900);
		assertThat(iniciar.ejecutar("U20260001", "temporal").estado())
			.isEqualTo(ResultadoInicioSesion.Estado.BLOQUEADO);
		assertThat(repositorio.buscarParaAutenticacion("U20260001").orElseThrow()
			.bloqueadoHasta()).isEqualTo(ahora.plusSeconds(900));
	}

	@Test
	void desbloqueaAlVencerElPlazoYReiniciaLosFallos() {
		EstadoAcceso bloqueada = new EstadoAcceso("U20260001", "hash:temporal",
			EstadoUsuario.BLOQUEADO, 5, ahora.minusSeconds(1), true,
			Set.of(new Rol("DOCENTE")));
		repositorio.guardarEstado(bloqueada);

		assertThat(iniciar.ejecutar("U20260001", "temporal").estado())
			.isEqualTo(ResultadoInicioSesion.Estado.EXITOSO);
		assertThat(repositorio.buscarParaAutenticacion("U20260001").orElseThrow()
			.intentosFallidos()).isZero();
	}

	@Test
	void noAutenticaCuentaInactivaNiRevelaSiNoExiste() {
		repositorio.guardarEstado(cuenta(EstadoUsuario.INACTIVO));
		assertThat(iniciar.ejecutar("U20260001", "temporal").estado())
			.isEqualTo(ResultadoInicioSesion.Estado.CREDENCIALES_INVALIDAS);
		assertThat(iniciar.ejecutar("desconocido", "temporal").estado())
			.isEqualTo(ResultadoInicioSesion.Estado.CREDENCIALES_INVALIDAS);
	}

	@Test
	void estadoDeAccesoNoExponeHashEnRegistros() {
		assertThat(cuenta(EstadoUsuario.ACTIVO).toString()).doesNotContain("hash:temporal");
	}

	private static EstadoAcceso cuenta(EstadoUsuario estado) {
		return new EstadoAcceso("U20260001", "hash:temporal", estado, 0, null, true,
			Set.of(new Rol("DOCENTE"), new Rol("APODERADO")));
	}

	private static final class RepositorioEnMemoria implements RepositorioAccesos {
		private final Map<String, EstadoAcceso> cuentas = new ConcurrentHashMap<>();

		@Override
		public Optional<EstadoAcceso> buscarParaAutenticacion(String nombreUsuario) {
			return Optional.ofNullable(cuentas.get(nombreUsuario));
		}

		@Override
		public void guardarEstado(EstadoAcceso acceso) {
			cuentas.put(acceso.nombreUsuario(), acceso);
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
