package com.galiledu.asistencia.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.galiledu.asistencia.aplicacion.puertos.RepositorioAsistenciaEstudiantes;
import com.galiledu.asistencia.dominio.EstadoAsistencia;
import com.galiledu.asistencia.dominio.RegistroAsistenciaEstudiante;

import static org.assertj.core.api.Assertions.assertThat;

class CorregirAsistenciaEstudianteTests {
	private static final LocalDate HOY = LocalDate.of(2027, 3, 15);
	private static final Clock RELOJ = Clock.fixed(
		Instant.parse("2027-03-15T15:00:00Z"), ZoneId.of("America/Lima"));
	private final RepositorioDePrueba registros = new RepositorioDePrueba();
	private final CorregirAsistenciaEstudiante caso = new CorregirAsistenciaEstudiante(
		registros, (actor, seccion) -> actor.equals("tutor") && seccion.equals("5A"), RELOJ);

	@Test
	void corrigeElEstadoDelDiaActualSiElTutorEstaAutorizado() {
		registros.guardar(new RegistroAsistenciaEstudiante("E1", "5A", HOY,
			EstadoAsistencia.AUSENTE));

		assertThat(caso.ejecutar("tutor", "E1", HOY, EstadoAsistencia.TARDANZA))
			.isEqualTo(CorregirAsistenciaEstudiante.Resultado.CORREGIDO);
		assertThat(registros.registro.estado()).isEqualTo(EstadoAsistencia.TARDANZA);
	}

	@Test
	void noModificaRegistrosDeDiasAnteriores() {
		var ayer = HOY.minusDays(1);
		registros.guardar(new RegistroAsistenciaEstudiante("E1", "5A", ayer,
			EstadoAsistencia.AUSENTE));

		assertThat(caso.ejecutar("tutor", "E1", ayer, EstadoAsistencia.PRESENTE))
			.isEqualTo(CorregirAsistenciaEstudiante.Resultado.FUERA_DEL_MISMO_DIA);
		assertThat(registros.registro.estado()).isEqualTo(EstadoAsistencia.AUSENTE);
	}

	@Test
	void noCorrigeSinAutorizacionNiRegistro() {
		registros.guardar(new RegistroAsistenciaEstudiante("E1", "5A", HOY,
			EstadoAsistencia.AUSENTE));

		assertThat(caso.ejecutar("ajeno", "E1", HOY, EstadoAsistencia.PRESENTE))
			.isEqualTo(CorregirAsistenciaEstudiante.Resultado.NO_DISPONIBLE);
		assertThat(caso.ejecutar("tutor", "otro", HOY, EstadoAsistencia.PRESENTE))
			.isEqualTo(CorregirAsistenciaEstudiante.Resultado.NO_DISPONIBLE);
		assertThat(registros.registro.estado()).isEqualTo(EstadoAsistencia.AUSENTE);
	}

	private static final class RepositorioDePrueba implements RepositorioAsistenciaEstudiantes {
		private RegistroAsistenciaEstudiante registro;

		@Override
		public Optional<RegistroAsistenciaEstudiante> buscar(String estudiante, LocalDate fecha) {
			return registro != null && registro.estudiante().equals(estudiante)
				&& registro.fecha().equals(fecha)
				? Optional.of(registro) : Optional.empty();
		}

		@Override
		public void guardar(RegistroAsistenciaEstudiante registro) {
			this.registro = registro;
		}
	}
}
